package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager;
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonToPartyPacket;
import com.cobblemon.mod.common.net.serverhandling.storage.pc.MovePCPokemonToPartyHandler;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.Bypass;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks moving a Pokémon from PC to party if it exceeds the player's cap.
 */
@Mixin(value = MovePCPokemonToPartyHandler.class, remap = false)
public abstract class MovePCPokemonToPartyHandlerMixin {

    @Inject(
            method = "handle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblelvlcap$blockOverCap(MovePCPokemonToPartyPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        if (CommandContext.inCommand()) return;
        // BYPASS OP / NON-SURVIVAL
        if (Bypass.shouldBypass(player)) return;

        if (!CapConfig.CFG.denyPcToPartyAboveCap) return;

        PCStore pc = PCLinkManager.INSTANCE.getPC(player);
        if (pc == null) return;

        PCPosition pos = packet.getPcPosition();
        Pokemon pokemon = pc.get(pos);
        if (pokemon == null) return;

        if (!pokemon.getUuid().equals(packet.getPokemonID())) return;

        int cap = LevelCapService.getCapFor(player);
        int level = pokemon.getLevel();
        if (level <= cap) return;

        player.sendMessage(
                Text.literal("Too high level for your party (max: " + cap + ")")
                        .formatted(Formatting.RED),
                true
        );
        player.closeHandledScreen();
        ci.cancel();
    }
}