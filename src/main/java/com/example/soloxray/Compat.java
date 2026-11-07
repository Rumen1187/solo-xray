package com.example.soloxray;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Every call that is likely to be renamed between Minecraft versions lives here,
 * so if an update breaks the build you only have to fix this one file.
 */
public final class Compat {
    private Compat() {}

    /** SAFETY GATE: only your own singleplayer / LAN-hosted world. Never a remote server. */
    public static boolean canUse(Minecraft mc) {
        return mc.getSingleplayerServer() != null && mc.getCurrentServer() == null;
    }

    /** Lowest chunk-section Y index of the level (e.g. -4 for the overworld). */
    public static int minSectionY(ClientLevel level) {
        return level.getMinSectionY();
    }

    /**
     * Client-only "ghost" item at the ore's position with a glowing outline.
     * Item entities are not targetable and have no collision, so they never get in the way of mining.
     */
    public static Entity spawnMarker(ClientLevel level, Hit hit, int fakeId) {
        ItemEntity marker = new ItemEntity(level,
                hit.pos().getX() + 0.5, hit.pos().getY() + 0.25, hit.pos().getZ() + 0.5,
                new ItemStack(hit.block()));
        marker.setDeltaMovement(Vec3.ZERO);
        marker.setNoGravity(true);
        marker.setGlowingTag(true);
        marker.setSilent(true);
        marker.setId(fakeId);

        // Outline color comes from the entity's scoreboard team color (client-side team only).
        Scoreboard scoreboard = level.getScoreboard();
        String teamName = "soloxray_" + hit.category().id();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(hit.category().color);
        }
        scoreboard.addPlayerToTeam(marker.getScoreboardName(), team);

        level.addEntity(marker);
        return marker;
    }

    public static void removeMarker(ClientLevel level, Entity marker) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(marker.getScoreboardName());
        if (team != null) scoreboard.removePlayerFromTeam(marker.getScoreboardName(), team);
        level.removeEntity(marker.getId(), Entity.RemovalReason.DISCARDED);
    }

    /** Keeps ChatFormatting import used even if only some code paths need it. */
    public static ChatFormatting colorOf(Category c) {
        return c.color;
    }
}
