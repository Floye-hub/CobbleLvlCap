package com.floye.cobblelvlcap;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OwnerTracker {
    private static final ConcurrentHashMap<UUID, ServerPlayerEntity> OWNER_BY_POKEMON_ID = new ConcurrentHashMap<>();
    private static volatile MinecraftServer LAST_SERVER;

    public static void start() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            LAST_SERVER = server; // garde une référence utilisable ailleurs
            if (server.getOverworld().getTime() % 20L == 0L) {
                refresh(server);
            }
        });
    }

    private static void refresh(MinecraftServer server) {
        OWNER_BY_POKEMON_ID.clear();
        var storage = Cobblemon.INSTANCE.getStorage();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            var party = storage.getParty(player);
            int size = party.size(); // si besoin, remplace par getSize()
            for (int i = 0; i < size; i++) {
                Pokemon pk = party.get(i);
                if (pk != null) OWNER_BY_POKEMON_ID.put(pk.getUuid(), player);
            }
        }
    }

    public static ServerPlayerEntity ownerOf(Pokemon pokemon) {
        return OWNER_BY_POKEMON_ID.get(pokemon.getUuid());
    }

    public static ServerPlayerEntity getPlayer(UUID uuid) {
        var s = LAST_SERVER;
        return s == null ? null : s.getPlayerManager().getPlayer(uuid);
    }
}