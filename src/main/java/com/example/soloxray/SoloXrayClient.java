package com.example.soloxray;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SoloXrayClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("solo-xray");

    private static final String[] DIRS = {
            "East", "South-East", "South", "South-West", "West", "North-West", "North", "North-East"
    };

    private static boolean enabled;
    private static boolean dirty;
    private static int radiusChunks = 3; // 1..4 (4 chunks = 64 blocks = vanilla entity render distance)
    private static int tick;
    private static final Set<Category> active = EnumSet.of(Category.DIAMOND, Category.NETHERITE);

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(SoloXrayClient::onTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("xray")
                        .executes(ctx -> toggle(ctx.getSource()))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("scan")
                                .executes(ctx -> scan(ctx.getSource())))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("targets")
                                .executes(ctx -> targets(ctx.getSource())))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("add")
                                .then(categoryArg().executes(ctx -> change(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "ore"), true))))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("remove")
                                .then(categoryArg().executes(ctx -> change(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "ore"), false))))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("radius")
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument(
                                                "chunks", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> {
                                            radiusChunks = IntegerArgumentType.getInteger(ctx, "chunks");
                                            dirty = true;
                                            ctx.getSource().sendFeedback(info("Scan radius: " + radiusChunks
                                                    + " chunks (" + radiusChunks * 16 + " blocks)."));
                                            return 1;
                                        })))));
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> categoryArg() {
        return RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("ore", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (Category c : Category.values()) builder.suggest(c.id());
                    return builder.buildFuture();
                });
    }

    // ---------------------------------------------------------------- commands

    private static int toggle(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled) {
            if (!Compat.canUse(mc) || mc.level == null) {
                src.sendError(Component.literal(
                        "X-ray only works in your own singleplayer/LAN-host world, not on servers."));
                return 0;
            }
            enabled = true;
            dirty = true;
            src.sendFeedback(info("X-ray ON. Outlines appear within about a second. Targets: " + targetNames()));
        } else {
            disable();
            src.sendFeedback(info("X-ray OFF."));
        }
        return 1;
    }

    private static int change(FabricClientCommandSource src, String id, boolean add) {
        Category c = Category.byId(id);
        if (c == null) {
            src.sendError(Component.literal("Unknown ore '" + id + "'. Options: " + allNames()));
            return 0;
        }
        if (add) active.add(c); else active.remove(c);
        dirty = true;
        src.sendFeedback(info((add ? "Added " : "Removed ") + c.label + ". Targets: " + targetNames()));
        return 1;
    }

    private static int targets(FabricClientCommandSource src) {
        src.sendFeedback(info("Targets: " + targetNames()));
        src.sendFeedback(info("Available: " + allNames()));
        return 1;
    }

    /** Text radar: nearest of each target ore, with distance and direction. */
    private static int scan(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !Compat.canUse(mc)) {
            src.sendError(Component.literal("X-ray only works in your own singleplayer/LAN-host world."));
            return 0;
        }
        List<Hit> hits = Scanner.scan(mc, active, radiusChunks);
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        Map<Category, Hit> nearest = new EnumMap<>(Category.class);
        for (Hit h : hits) { // hits are sorted nearest first
            counts.merge(h.category(), 1, Integer::sum);
            nearest.putIfAbsent(h.category(), h);
        }
        src.sendFeedback(Component.literal("[X-Ray] Scan, radius " + radiusChunks + " chunks:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (Category c : active) {
            MutableComponent line = Component.empty().append(
                    Component.literal(" - " + c.label + ": ").withStyle(c.color, ChatFormatting.BOLD));
            Hit h = nearest.get(c);
            if (h == null) {
                line.append(Component.literal("none loaded nearby").withStyle(ChatFormatting.GRAY));
            } else {
                double dx = h.pos().getX() + 0.5 - p.getX(), dz = h.pos().getZ() + 0.5 - p.getZ();
                long dist = Math.round(Math.sqrt(dx * dx + dz * dz));
                double angle = Math.toDegrees(Math.atan2(dz, dx));
                String dir = DIRS[(int) Math.round(((angle + 360) % 360) / 45.0) % 8];
                line.append(Component.literal(counts.get(c) + " found, nearest " + dist + " blocks " + dir
                        + " at Y " + h.pos().getY()).withStyle(ChatFormatting.WHITE));
                line.append(Component.literal("  (" + h.pos().getX() + ", " + h.pos().getY() + ", "
                        + h.pos().getZ() + ")").withStyle(ChatFormatting.YELLOW));
            }
            src.sendFeedback(line);
        }
        src.sendFeedback(Component.literal("Only chunks your client has loaded are scanned.")
                .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    // ---------------------------------------------------------------- logic

    private static void onTick(Minecraft mc) {
        if (!enabled) return;
        if (mc.level == null || mc.player == null || !Compat.canUse(mc)) {
            Markers.forget();
            enabled = false;
            return;
        }
        tick++;
        if (dirty || tick % 20 == 0) { // rescan once per second; picks up mined ores too
            dirty = false;
            Markers.sync(mc.level, Scanner.scan(mc, active, radiusChunks));
        }
    }

    private static void disable() {
        enabled = false;
        Markers.clear(Minecraft.getInstance().level);
    }

    private static void reset() {
        enabled = false;
        dirty = false;
        Markers.forget();
    }

    // ---------------------------------------------------------------- text

    private static MutableComponent info(String text) {
        return Component.empty()
                .append(Component.literal("[X-Ray] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    private static String targetNames() {
        if (active.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (Category c : active) sb.append(sb.length() == 0 ? "" : ", ").append(c.id());
        return sb.toString();
    }

    private static String allNames() {
        StringBuilder sb = new StringBuilder();
        for (Category c : Category.values()) sb.append(sb.length() == 0 ? "" : ", ").append(c.id());
        return sb.toString();
    }
}
