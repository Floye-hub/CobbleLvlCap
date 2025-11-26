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
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ActiveTrade.class, remap = false)
public abstract class ActiveTradeMixin {

    @Inject(method = "updateAcceptance", at = @At("HEAD"), cancellable = true)
    private void cobblelvlcap$blockAcceptanceIfOverCap(TradeParticipant tradeParticipant, boolean acceptance, CallbackInfo ci) {
        if (!CapConfig.CFG.enableTradeCap) return;
        if (!acceptance) return;

        ActiveTrade self = (ActiveTrade) (Object) this;

        TradeOffer opposingOffer = self.getOpposingOffer(tradeParticipant);
        Pokemon offered = opposingOffer.getPokemon();
        if (offered == null) return;

        ServerPlayerEntity receiver = OwnerTracker.getPlayer(tradeParticipant.getUuid());
        int cap = LevelCapService.getCapFor(receiver);

        if (offered.getLevel() > cap && receiver != null) {
            receiver.sendMessage(
                    Text.literal("Trade cancelled (max level: " + cap + ")")
                            .formatted(Formatting.RED),
                    true
            );
            ci.cancel();
        }
    }

    @Inject(method = "performTrade", at = @At("HEAD"), cancellable = true)
    private void cobblelvlcap$checkBeforePerform(TradeParticipant tradeParticipant, CallbackInfo ci) {
        if (!CapConfig.CFG.enableTradeCap) return;

        ActiveTrade self = (ActiveTrade) (Object) this;

        Pokemon p1 = self.getPlayer1Offer().getPokemon();
        Pokemon p2 = self.getPlayer2Offer().getPokemon();

        ServerPlayerEntity player1 = OwnerTracker.getPlayer(self.getPlayer1().getUuid());
        ServerPlayerEntity player2 = OwnerTracker.getPlayer(self.getPlayer2().getUuid());

        if (p1 != null && player2 != null) {
            int cap2 = LevelCapService.getCapFor(player2);
            if (p1.getLevel() > cap2) {
                player2.sendMessage(
                        Text.literal("Trade cancelled (max level: " + cap2 + ")")
                                .formatted(Formatting.RED),
                        true
                );
                ci.cancel();
                return;
            }
        }

        if (p2 != null && player1 != null) {
            int cap1 = LevelCapService.getCapFor(player1);
            if (p2.getLevel() > cap1) {
                player1.sendMessage(
                        Text.literal("Trade cancelled (max level: " + cap1 + ")")
                                .formatted(Formatting.RED),
                        true
                );
                ci.cancel();
            }
        }
    }
}