package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.trade.ActiveTrade;
import com.cobblemon.mod.common.trade.TradeOffer;
import com.cobblemon.mod.common.trade.TradeParticipant;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.OwnerTracker;
import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ActiveTrade.class, remap = false)
public abstract class ActiveTradeMixin {

    // Intercepte la tentative d’acceptation (bouton "Prêt")
    @Inject(method = "updateAcceptance", at = @At("HEAD"), cancellable = true)
    private void cobblelvlcap$blockAcceptanceIfOverCap(TradeParticipant tradeParticipant, boolean acceptance, CallbackInfo ci) {
        if (!CapConfig.CFG.enableTradeCap) return;
        if (!acceptance) return; // on ne bloque que lorsqu’on tente d’accepter

        ActiveTrade self = (ActiveTrade) (Object) this;

        // Le joueur "tradeParticipant" accepte l’offre de l’autre -> on récupère le Pokémon offert par l’autre
        TradeOffer opposingOffer = self.getOpposingOffer(tradeParticipant);
        Pokemon offered = opposingOffer.getPokemon();
        if (offered == null) return;

        // Recepteur = le joueur qui clique "Prêt"
        ServerPlayerEntity receiver = OwnerTracker.getPlayer(tradeParticipant.getUuid());
        int cap = LevelCapService.getCapFor(receiver);

        if (offered.getLevel() > cap) {
            // Refus: le Pokémon dépasse le level max autorisé par les tags du receveur
            if (receiver != null) {
                receiver.sendMessage(Text.literal("Trade refusé: le Pokémon (" + offered.getSpecies().getName() + ") dépasse votre limite de niveau (" + cap + ")."));
            }
            ci.cancel(); // ne pas marquer l’acceptation
        }
    }

    // Double garde: juste avant l’échange effectif
    @Inject(method = "performTrade", at = @At("HEAD"), cancellable = true)
    private void cobblelvlcap$checkBeforePerform(TradeParticipant tradeParticipant, CallbackInfo ci) {
        if (!CapConfig.CFG.enableTradeCap) return;

        ActiveTrade self = (ActiveTrade) (Object) this;

        // Récupère les 2 offres (variables non utilisées, conservées si besoin de debug)
        TradeOffer offer1 = self.getOffer(self.getOppositePlayer(self.getOppositePlayer(tradeParticipant))); // player1Offer
        TradeOffer offer2 = self.getOpposingOffer(self.getOppositePlayer(self.getOppositePlayer(tradeParticipant))); // player2Offer

        Pokemon p1 = self.getPlayer1Offer().getPokemon();
        Pokemon p2 = self.getPlayer2Offer().getPokemon();

        // Récupère les receveurs: p1 est reçu par player2, p2 est reçu par player1
        ServerPlayerEntity player1 = OwnerTracker.getPlayer(self.getPlayer1().getUuid());
        ServerPlayerEntity player2 = OwnerTracker.getPlayer(self.getPlayer2().getUuid());

        if (p1 != null && player2 != null) {
            int cap2 = LevelCapService.getCapFor(player2);
            if (p1.getLevel() > cap2) {
                player2.sendMessage(Text.literal("Trade refusé: le Pokémon (" + p1.getSpecies().getName() + ") dépasse votre limite de niveau (" + cap2 + ")."));
                ci.cancel();
                return;
            }
        }

        if (p2 != null && player1 != null) {
            int cap1 = LevelCapService.getCapFor(player1);
            if (p2.getLevel() > cap1) {
                player1.sendMessage(Text.literal("Trade refusé: le Pokémon (" + p2.getSpecies().getName() + ") dépasse votre limite de niveau (" + cap1 + ")."));
                ci.cancel();
            }
        }
    }
}