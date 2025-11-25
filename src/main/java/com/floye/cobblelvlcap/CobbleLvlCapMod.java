package com.floye.cobblelvlcap;

import com.floye.cobblelvlcap.config.CapConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class CobbleLvlCapMod implements ModInitializer {
    public static final String MOD_ID = "cobblelvlcap";

    @Override
    public void onInitialize() {
        CapConfig.load();
        OwnerTracker.start();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("cobblelvlcap")
                            .then(CommandManager.literal("reload").executes(ctx -> {
                                CapConfig.load();
                                ctx.getSource().sendFeedback(() -> Text.literal("[CobbleLvlCap] Config reloaded"), false);
                                return 1;
                            }))
            );
        });
    }
}