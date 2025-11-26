package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.OriginalTrainerType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.OwnerTracker;
import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Capture / direct party add:
 * - If Pokémon is within the capture cap -> let Cobblemon handle it (party + overflow).
 * - If Pokémon is above the capture cap:
 *   - denyCaptureAboveCap = true  -> cancel capture completely.
 *   - denyCaptureAboveCap = false -> capture goes straight to PC instead of the party.
 */
@Mixin(value = PlayerPartyStore.class, remap = false)
public abstract class PlayerPartyStoreAddMixin {

    @Inject(
            method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblelvlcap$onAdd(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
        if (pokemon == null) return;
        if (CommandContext.inCommand()) return;

        CapConfig.CapModel cfg = CapConfig.CFG;

        PlayerPartyStore self = (PlayerPartyStore) (Object) this;
        UUID playerUUID = self.getPlayerUUID();
        ServerPlayerEntity player = OwnerTracker.getPlayer(playerUUID);
        if (player == null) return;

        int cap = getCaptureCap(player, self, cfg);
        int level = pokemon.getLevel();

        // Within cap -> let original logic run
        if (level <= cap) return;

        // Above cap
        if (cfg.denyCaptureAboveCap) {
            player.sendMessage(
                    Text.literal("Capture cancelled (max: " + cap + ")")
                            .formatted(Formatting.RED),
                    true
            );
            cir.setReturnValue(false);
            return;
        }

        // Above cap, capture allowed but forced to PC
        if (pokemon.getOriginalTrainerType() == OriginalTrainerType.NONE) {
            pokemon.setOriginalTrainer(playerUUID);
        }
        pokemon.refreshOriginalTrainer();

        MinecraftServer server = player.getServer();
        if (server == null) {
            cir.setReturnValue(false);
            return;
        }
        DynamicRegistryManager registryManager = server.getRegistryManager();

        PCStore pc = self.getOverflowPC(registryManager);
        if (pc == null || !pc.add(pokemon)) {
            player.sendMessage(
                    Text.literal("PC is full or unavailable, capture was not stored (max: " + cap + ")")
                            .formatted(Formatting.RED),
                    true
            );
            cir.setReturnValue(false);
            return;
        }

        player.sendMessage(
                Text.literal("Captured Pokémon sent to PC (max: " + cap + ")")
                        .formatted(Formatting.RED),
                true
        );
        cir.setReturnValue(true);
    }

    private int getCaptureCap(ServerPlayerEntity player, PartyStore party, CapConfig.CapModel cfg) {
        if (cfg.usePartyHighestAsCaptureCap) {
            int max = 0;
            int size;
            try {
                size = party.size();
            } catch (Throwable t) {
                size = 6;
            }

            for (int i = 0; i < size; i++) {
                Pokemon p = party.get(i);
                if (p != null && p.getLevel() > max) {
                    max = p.getLevel();
                }
            }

            return (max <= 0) ? Integer.MAX_VALUE : max;
        } else {
            return LevelCapService.getCapFor(player);
        }
    }
}