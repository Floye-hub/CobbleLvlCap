package com.floye.cobblelvlcap.mixin;

import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.CommandContext;
import com.floye.cobblelvlcap.LevelCapService;
import com.floye.cobblelvlcap.OwnerTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**

 Cappe le niveau des Pokémon ajoutés à la party d'un joueur (capture, etc.).

 Injection optionnelle sur add(Pokemon): void et add(Pokemon): boolean selon la build.
 */
@Mixin(value = PlayerPartyStore.class, remap = false)
public abstract class PlayerPartyStoreAddMixin {

    // Variante void: add(Pokemon): Unit
    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$capLevelOnAddVoid(Pokemon pokemon, CallbackInfo ci) {
        capLevelIfNeeded(pokemon);
    }

    // Variante boolean: add(Pokemon): Boolean
    @Inject(method = "add(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z", at = @At("HEAD"), require = 0)
    private void cobblelvlcap$capLevelOnAddBool(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
        capLevelIfNeeded(pokemon);
    }

    private void capLevelIfNeeded(Pokemon pokemon) {
        if (pokemon == null) return;
// Bypass complet si via commande
        if (CommandContext.inCommand()) return;

        // Récupère le joueur propriétaire de cette party via l'UUID stockée dans PartyStore
        PartyStore store = (PartyStore) (Object) this;
        UUID ownerId = store.getUuid();
        ServerPlayerEntity player = OwnerTracker.getPlayer(ownerId);
        if (player == null) return;

        int cap = LevelCapService.getCapFor(player);
        if (pokemon.getLevel() > cap) {
            try {
                pokemon.setLevel(cap);
                player.sendMessage(Text.literal(
                        "Capture: le niveau de " + pokemon.getSpecies().getName() + " a été réduit à " + cap + " pour respecter votre limite."
                ));
            } catch (Throwable t) {
                // Si setLevel diffère dans ta build, dis-moi le nom/params exacts et j'ajuste.
            }
        }
    }

}