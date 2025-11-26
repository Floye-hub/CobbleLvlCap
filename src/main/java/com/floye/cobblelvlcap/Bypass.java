package com.floye.cobblelvlcap;

import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Bypass {

    private Bypass() {}

    /** True si ce joueur doit ignorer toutes les restrictions CobbleLvlCap (OP / non-survie). */
    public static boolean shouldBypass(ServerPlayerEntity player) {
        if (!CapConfig.CFG.allowOpGamemodeBypass) return false;
        if (player == null) return false;

        // OP (permission level >= 2)
        if (player.hasPermissionLevel(2)) return true;

        // Non-survival (creative, spectator)
        if (player.isCreative() || player.isSpectator()) return true;

        return false;
    }
}