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
        public String defaultCap_comment = "Base level cap used when the player has no cap tags.";

        // Mapping tag -> cap
        public Map<String, Integer> byTag = Map.of(
                "cap_20", 20,
                "cap_30", 30
        );
        public String byTag_comment = "Map of player command tags to their level caps. Example: using '/tag <player> add cap_20' gives them a cap of 20.";

        // XP from commands can bypass cap
        public boolean allowCommandBypass = true;
        public String allowCommandBypass_comment = "If true, all actions performed while running commands ignore level caps (XP, capture, moves, trades).";

        // XP gain limit
        public boolean enableExperienceCap = true;
        public String enableExperienceCap_comment = "If true, party Pokémon stop gaining XP once they reach the level cap (based on tags/defaultCap).";

        // Trade restriction
        public boolean enableTradeCap = true;
        public String enableTradeCap_comment = "If true, trades are cancelled if the Pokémon received is above the receiver's tag-based level cap.";

        /** NEW: bypass for OPs / non-survival */
        public boolean allowOpGamemodeBypass = true;
        public String allowOpGamemodeBypass_comment = "If true, operators (permission level >= 2) and players not in survival mode are not affected by level caps.";

        /**
         * If true, any capture of a Pokémon above the capture cap is fully denied.
         * The Pokémon will not be added to the party or the PC.
         */
        public boolean denyCaptureAboveCap = false;
        public String denyCaptureAboveCap_comment = "If true, captures above the capture cap are completely denied (Pokémon is not stored anywhere).";

        /**
         * If true, moving a Pokémon from PC -> party is forbidden when it is above
         * the player's tag-based cap. The Pokémon stays in the PC.
         */
        public boolean denyPcToPartyAboveCap = true;
        public String denyPcToPartyAboveCap_comment = "If true, you cannot move PC Pokémon above your tag-based cap into your party.";

        /**
         * If true, the capture cap uses the highest level in the current party instead
         * of the tag-based cap.
         */
        public boolean usePartyHighestAsCaptureCap = false;
        public String usePartyHighestAsCaptureCap_comment = "If true, your highest party level becomes your capture cap instead of using tags/defaultCap.";
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