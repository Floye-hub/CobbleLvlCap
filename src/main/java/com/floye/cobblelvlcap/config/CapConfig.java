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
        // Mapping tag -> cap. Ex: "cap_20": 20, "badge_1": 25, etc.
        public Map<String, Integer> byTag = Map.of(
                "cap_20", 20,
                "cap_30", 30
        );
        // XP issue d'une commande bypass le cap
        public boolean allowCommandBypass = true;
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