package com.floye.cobblelvlcap;

import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class LevelCapService {
    public static int getCapFor(ServerPlayerEntity player) {
        if (player == null) return Math.max(CapConfig.CFG.defaultCap, 1);
        Integer best = null;
        for (String tag : player.getCommandTags()) { // Yarn
            Integer v = CapConfig.CFG.byTag.get(tag);
            if (v != null && (best == null || v > best)) best = v;
        }
        int base = CapConfig.CFG.defaultCap;
        return Math.max(best != null ? best : base, 1);
    }

    public static int getCapForUuid(UUID uuid) {
        return getCapFor(OwnerTracker.getPlayer(uuid));
    }
}