package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.OwnerTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.UUID;

/**

 Réduction de niveau quand un Pokémon est ajouté au PC (équipe pleine).

 Soft-target (pseudo) pour éviter tout crash si la classe diffère dans ta build Cobblemon.
 */
@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.api.storage.pc.PlayerPCStore", remap = false)
public abstract class PlayerPCStoreAddMixin {

    // Variante add(Pokemon): void
    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$capLevelOnAddVoid(Pokemon pokemon, CallbackInfo ci) {
        capLevelIfNeeded(pokemon);
    }

    // Variante add(Pokemon): boolean
    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$capLevelOnAddBool(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
        capLevelIfNeeded(pokemon);
    }

    private void capLevelIfNeeded(Pokemon pokemon) {
        if (pokemon == null) return;
        if (CommandContext.inCommand()) return; // bypass commandes

        // Récupère l'UUID propriétaire du store via réflexion
        UUID ownerId = getOwnerUuidReflect((Object) this);
        if (ownerId == null) return;

        ServerPlayerEntity player = OwnerTracker.getPlayer(ownerId);
        if (player == null) return;

        int cap = LevelCapService.getCapFor(player);
        if (pokemon.getLevel() > cap) {
            try {
                pokemon.setLevel(cap);
                player.sendMessage(Text.literal(
                        "Capture (PC): le niveau de " + pokemon.getSpecies().getName() + " a été réduit à " + cap + " pour respecter votre limite."
                ));
            } catch (Throwable ignored) {
                // Si setLevel diffère dans ta build, dis-moi la signature exacte et j’adapte.
            }
        }
    }

    private static UUID getOwnerUuidReflect(Object store) {
        try {
            Method m = store.getClass().getMethod("getUuid");
            Object o = m.invoke(store);
            if (o instanceof UUID u) return u;
        } catch (Throwable ignored) {}
        try {
            Method m = store.getClass().getMethod("getOwnerUuid");
            Object o = m.invoke(store);
            if (o instanceof UUID u) return u;
        } catch (Throwable ignored) {}
        try {
            Method m = store.getClass().getMethod("getPlayerUuid");
            Object o = m.invoke(store);
            if (o instanceof UUID u) return u;
        } catch (Throwable ignored) {}
        return null;
    }

}