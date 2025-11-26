package com.floye.cobblelvlcap;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.pokemon.PokemonGainedEvent;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.config.CapConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class CaptureHandler {

    public static void onPokemonGained(PokemonGainedEvent event) {
        UUID playerId = event.getPlayerId();
        ServerPlayerEntity player = OwnerTracker.getPlayer(playerId);
        if (player == null) return;

        // Bypass cap si via commande
        if (CommandContext.inCommand()) return;

        CapConfig.CapModel cfg = CapConfig.CFG;
        Pokemon gained = event.getPokemon();
        int level = gained.getLevel();

        // Récupère la party du joueur
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        // Cap de capture
        int cap = getCaptureCap(player, party, gained, cfg);

        // Si dans le cap, on laisse Cobblemon faire normalement
        if (level <= cap) {
            return;
        }

        // Ici : niveau > cap_capture
        if (cfg.denyCaptureAboveCap) {
            // Annuler la capture : retirer le Pokémon de la party et/ou du PC
            boolean removedFromParty = party.remove(gained);

            if (!removedFromParty && party instanceof PlayerPartyStore ps) {
                // Peut-être que Cobblemon l’a mis directement dans le PC (overflow)
                MinecraftServer server = player.getServer();
                if (server != null) {
                    PCStore pc = ps.getOverflowPC(server.getRegistryManager());
                    if (pc != null) {
                        pc.remove(gained);
                    }
                }
            }

            player.sendMessage(Text.literal(
                    "Capture annulée : " + gained.getSpecies().getName() +
                            " (niveau " + level + ") dépasse votre limite (" + cap + ")."
            ));
        } else {
            // Capture > cap autorisée, mais si le Pokémon est dans l'équipe, on le déplace dans le PC
            moveToPcIfInParty(player, party, gained, level, cap);
        }
    }

    private static int getCaptureCap(ServerPlayerEntity player, PartyStore party, Pokemon gained, CapConfig.CapModel cfg) {
        if (cfg.usePartyHighestAsCaptureCap) {
            // Cap basé sur le niveau max actuel de la team (hors Pokémon tout juste gagné)
            int max = 0;
            int size;
            try {
                size = party.size();
            } catch (Throwable t) {
                size = 6;
            }

            for (int i = 0; i < size; i++) {
                Pokemon p = party.get(i);
                if (p == null) continue;
                // on ignore le Pokémon qui vient d'être gagné
                if (p.getUuid().equals(gained.getUuid())) continue;

                if (p.getLevel() > max) {
                    max = p.getLevel();
                }
            }

            // Si aucune team ou max == 0 : pas de limite pour la capture
            return (max <= 0) ? Integer.MAX_VALUE : max;
        } else {
            // Cap classique par tags
            return LevelCapService.getCapFor(player);
        }
    }

    private static void moveToPcIfInParty(ServerPlayerEntity player,
                                          PartyStore party,
                                          Pokemon pokemon,
                                          int level,
                                          int cap) {

        // Vérifie si le Pokémon est actuellement dans l'équipe
        boolean inParty = false;
        int size;
        try {
            size = party.size();
        } catch (Throwable t) {
            size = 6;
        }

        for (int i = 0; i < size; i++) {
            Pokemon p = party.get(i);
            if (p != null && p.getUuid().equals(pokemon.getUuid())) {
                inParty = true;
                break;
            }
        }

        if (!inParty) {
            // Il est déjà dans le PC (overflow Cobblemon) ou ailleurs -> ne rien faire
            return;
        }

        if (!(party instanceof PlayerPartyStore ps)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        PCStore pc = ps.getOverflowPC(server.getRegistryManager());
        if (pc == null) {
            player.sendMessage(Text.literal(
                    "Capture : votre PC est plein ou indisponible, " +
                            pokemon.getSpecies().getName() +
                            " reste dans votre équipe malgré le dépassement du cap (" + cap + ")."
            ));
            return;
        }

        // Retirer de l’équipe
        party.remove(pokemon);

        // Ajouter au PC
        boolean addedToPc = pc.add(pokemon);
        if (addedToPc) {
            player.sendMessage(Text.literal(
                    "Le Pokémon " + pokemon.getSpecies().getName() +
                            " (niveau " + level + ") dépasse votre limite (" + cap +
                            "). Il a été envoyé dans votre PC."
            ));
        } else {
            // En cas d'échec, on le remet dans l’équipe
            party.add(pokemon);
            player.sendMessage(Text.literal(
                    "Capture : impossible de déplacer " + pokemon.getSpecies().getName() +
                            " dans votre PC, il reste dans votre équipe."
            ));
        }
    }
}