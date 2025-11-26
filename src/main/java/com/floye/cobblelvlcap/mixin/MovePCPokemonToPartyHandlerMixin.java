package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager;
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonToPartyPacket;
import com.cobblemon.mod.common.net.serverhandling.storage.pc.MovePCPokemonToPartyHandler;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bloque le mouvement PC -> équipe si le Pokémon dépasse le cap :
 * - On ne retire pas le Pokémon du PC.
 * - On ne l’ajoute pas à l’équipe.
 * - Le Pokémon reste donc dans le PC, sans duplication ni disparition.
 */
@Mixin(value = MovePCPokemonToPartyHandler.class, remap = false)
public abstract class MovePCPokemonToPartyHandlerMixin {

    @Inject(
            method = "handle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblelvlcap$blockOverCap(MovePCPokemonToPartyPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        // Bypass si via commande
        if (CommandContext.inCommand()) return;

        // Si on ne veut pas bloquer PC -> équipe, on sort
        if (!CapConfig.CFG.denyPcToPartyAboveCap) return;

        PCStore pc = PCLinkManager.INSTANCE.getPC(player);
        if (pc == null) {
            return;
        }

        PCPosition pos = packet.getPcPosition();
        Pokemon pokemon = pc.get(pos);
        if (pokemon == null) {
            return;
        }

        // Vérifie qu'on parle bien du bon Pokémon
        if (!pokemon.getUuid().equals(packet.getPokemonID())) {
            return;
        }

        int cap = LevelCapService.getCapFor(player);
        int level = pokemon.getLevel();
        if (level <= cap) {
            // Niveau OK, on laisse la logique Cobblemon gérer
            return;
        }

        // Niveau > cap : on bloque le mouvement
        player.sendMessage(Text.literal(
                "Déplacement annulé : " + pokemon.getSpecies().getName() +
                        " (niveau " + level + ") dépasse votre limite (" + cap +
                        "). Le Pokémon reste dans votre PC."
        ));
        ci.cancel();
    }
}