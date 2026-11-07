package com.example.soloxray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** One ore block found by the scanner. */
public record Hit(BlockPos pos, Category category, Block block) {
}
