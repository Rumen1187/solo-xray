package com.example.soloxray;

import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Ore groups you can turn on/off. The color is also the outline color in the world. */
public enum Category {
    DIAMOND("Diamond", ChatFormatting.AQUA, s -> s.is(BlockTags.DIAMOND_ORES)),
    NETHERITE("Netherite (ancient debris)", ChatFormatting.LIGHT_PURPLE, s -> s.is(Blocks.ANCIENT_DEBRIS)),
    EMERALD("Emerald", ChatFormatting.GREEN, s -> s.is(BlockTags.EMERALD_ORES)),
    GOLD("Gold", ChatFormatting.YELLOW, s -> s.is(BlockTags.GOLD_ORES)),
    REDSTONE("Redstone", ChatFormatting.RED, s -> s.is(BlockTags.REDSTONE_ORES)),
    LAPIS("Lapis", ChatFormatting.BLUE, s -> s.is(BlockTags.LAPIS_ORES)),
    IRON("Iron", ChatFormatting.WHITE, s -> s.is(BlockTags.IRON_ORES)),
    COPPER("Copper", ChatFormatting.GOLD, s -> s.is(BlockTags.COPPER_ORES)),
    COAL("Coal", ChatFormatting.DARK_GRAY, s -> s.is(BlockTags.COAL_ORES)),
    QUARTZ("Nether quartz", ChatFormatting.GRAY, s -> s.is(Blocks.NETHER_QUARTZ_ORE));

    public final String label;
    public final ChatFormatting color;
    private final Predicate<BlockState> test;

    Category(String label, ChatFormatting color, Predicate<BlockState> test) {
        this.label = label;
        this.color = color;
        this.test = test;
    }

    public String id() {
        return name().toLowerCase();
    }

    /** First active category that matches this block, or null. */
    public static Category of(BlockState state, Set<Category> active) {
        for (Category c : active) {
            if (c.test.test(state)) return c;
        }
        return null;
    }

    public static Category byId(String id) {
        for (Category c : values()) {
            if (c.id().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}
