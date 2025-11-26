package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ne fait plus rien : aucun clamp dans le PC.
 */
@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.api.storage.pc.PlayerPCStore", remap = false)
public abstract class PlayerPCStoreAddMixin {

    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$onAddVoid(Pokemon pokemon, CallbackInfo ci) {
        // no-op
    }

    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$onAddBool(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
        // no-op
    }
}