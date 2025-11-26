package com.floye.cobblelvlcap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("PKH/cobblelvlcap.json");

    public static volatile CapModel CFG = new CapModel();

    public static class CapModel {
        public int defaultCap = 100;
        // Mapping tag -> cap
        public Map<String, Integer> byTag = Map.of(
                "cap_20", 20,
                "cap_30", 30
        );

        // XP issue d'une commande bypass le cap
        public boolean allowCommandBypass = true;

        // Limiter l'XP (PokemonAddExperienceMixin)
        public boolean enableExperienceCap = true;

        // Anciennes options de clamp (gardées pour compat, mais plus utilisées)
        public boolean enableCaptureClampParty = true;
        public boolean enableCaptureClampPc = true;

        // Bloquer/valider les échanges suivant le cap
        public boolean enableTradeCap = true;

        /**
         * Si true, une capture d'un Pokémon au-dessus du cap est purement annulée.
         * -> le Pokémon n'est pas ajouté à la party ni au PC.
         */
        public boolean denyCaptureAboveCap = false;

        /**
         * Si true, un déplacement PC -> équipe d'un Pokémon au-dessus du cap est interdit :
         * -> le handler serveur annule le move, le Pokémon reste dans le PC.
         */
        public boolean denyPcToPartyAboveCap = true;

        /**
         * Si true, pour les captures on utilise le niveau le plus élevé de l'équipe
         * comme cap, au lieu du cap basé sur les tags.
         *
         * - false : capture utilise LevelCapService (tags + defaultCap).
         * - true  : capture utilise max(levels de la party). Si l'équipe est vide,
         *           on considère qu'il n'y a pas de limite (cap très élevé).
         */
        public boolean usePartyHighestAsCaptureCap = false;
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                saveDefault();
            }
            try (Reader r = Files.newBufferedReader(PATH)) {
                CapModel read = GSON.fromJson(r, CapModel.class);
                if (read != null) CFG = read;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDefault() throws IOException {
        Files.createDirectories(PATH.getParent());
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(new CapModel(), w);
        }
    }
}