package com.floye.cobblelvlcap.mixin;

import com.floye.cobblelvlcap.Bypass;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.OwnerTracker;
import com.floye.cobblelvlcap.config.CapConfig;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Pokemon.class)
public abstract class PokemonAddExperienceMixin {

    @ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true)
    private int cobblelvlcap$clampExp(int amount) {
        if (!CapConfig.CFG.enableExperienceCap) return amount;
        if (CommandContext.inCommand()) return amount;

        Pokemon pk = (Pokemon) (Object) this;

        ServerPlayerEntity owner = OwnerTracker.ownerOf(pk);
        if (owner == null) return amount;

        // BYPASS OP / NON-SURVIVAL
        if (Bypass.shouldBypass(owner)) return amount;

        int cap = LevelCapService.getCapFor(owner);

        if (pk.getLevel() >= cap) return 0;

        return amount;
    }
}