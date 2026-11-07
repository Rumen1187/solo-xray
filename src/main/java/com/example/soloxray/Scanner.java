package com.example.soloxray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Finds ore blocks in the chunks the client has loaded around you. */
public final class Scanner {
    private Scanner() {}

    /** Returns every matching block within radiusChunks, nearest first. */
    public static List<Hit> scan(Minecraft mc, Set<Category> active, int radiusChunks) {
        List<Hit> out = new ArrayList<>();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || active.isEmpty()) return out;

        int pcx = player.blockPosition().getX() >> 4;
        int pcz = player.blockPosition().getZ() >> 4;
        int minSectionY = Compat.minSectionY(level);
        // Cheap per-section palette check so empty/ore-free sections are skipped.
        Predicate<BlockState> anyTarget = s -> Category.of(s, active) != null;

        for (int cx = pcx - radiusChunks; cx <= pcx + radiusChunks; cx++) {
            for (int cz = pcz - radiusChunks; cz <= pcz + radiusChunks; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir() || !section.maybeHas(anyTarget)) continue;
                    int baseY = (minSectionY + i) << 4;
                    for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int lx = 0; lx < 16; lx++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                Category c = Category.of(state, active);
                                if (c != null) {
                                    out.add(new Hit(new BlockPos((cx << 4) + lx, baseY + ly, (cz << 4) + lz),
                                            c, state.getBlock()));
                                }
                            }
                        }
                    }
                }
            }
        }

        double px = player.getX(), py = player.getY(), pz = player.getZ();
        out.sort(Comparator.comparingDouble(h -> sq(h.pos().getX() + 0.5 - px)
                + sq(h.pos().getY() + 0.5 - py) + sq(h.pos().getZ() + 0.5 - pz)));
        return out;
    }

    private static double sq(double v) {
        return v * v;
    }
}
