package com.floye.cobblelvlcap;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.floye.cobblelvlcap.config.CapConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
                            .then(CommandManager.literal("info")
                                    .executes(ctx -> {
                                        ServerCommandSource src = ctx.getSource();
                                        ServerPlayerEntity self;
                                        try {
                                            self = src.getPlayer();
                                        } catch (Exception e) {
                                            src.sendFeedback(() -> Text.literal("[CobbleLvlCap] This command must be executed by a player."), false);
                                            return 0;
                                        }
                                        sendInfo(src, self);
                                        return 1;
                                    })
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .requires(s -> s.hasPermissionLevel(2))
                                            .executes(ctx -> {
                                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                sendInfo(ctx.getSource(), target);
                                                return 1;
                                            })
                                    )
                            )
            );
        });
    }

    /** Capture cap, matching the capture mixin logic. */
    private static int getCaptureCap(ServerPlayerEntity player) {
        CapConfig.CapModel cfg = CapConfig.CFG;
        if (cfg.usePartyHighestAsCaptureCap) {
            PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            int max = 0;
            int size;
            try {
                size = party.size();
            } catch (Throwable t) {
                size = 6;
            }
            for (int i = 0; i < size; i++) {
                Pokemon p = party.get(i);
                if (p != null && p.getLevel() > max) {
                    max = p.getLevel();
                }
            }
            return (max <= 0) ? Integer.MAX_VALUE : max;
        } else {
            return LevelCapService.getCapFor(player);
        }
    }

    private static void sendInfo(ServerCommandSource src, ServerPlayerEntity target) {
        int capCapture = getCaptureCap(target);
        String capText = (capCapture == Integer.MAX_VALUE) ? "no limit" : String.valueOf(capCapture);

        src.sendFeedback(
                () -> Text.literal("[CobbleLvlCap] Max capture level for " + target.getName().getString() + ": " + capText),
                false
        );
    }
}