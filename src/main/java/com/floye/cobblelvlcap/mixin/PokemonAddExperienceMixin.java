package com.floye.cobblelvlcap.mixin;

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
        // Option globale
        if (!CapConfig.CFG.enableExperienceCap) return amount;

        // Bypass si via commande
        if (CommandContext.inCommand()) return amount;

        Pokemon pk = (Pokemon) (Object) this;

        // On ne cappe que les Pokémon de la team d'un joueur
        ServerPlayerEntity owner = OwnerTracker.ownerOf(pk);
        if (owner == null) return amount;

        int cap = LevelCapService.getCapFor(owner);

        // Déjà au cap => toute XP est perdue
        if (pk.getLevel() >= cap) return 0;

        // Sinon, on laisse passer l'XP
        return amount;
    }
}