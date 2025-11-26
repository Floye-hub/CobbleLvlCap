package com.floye.cobblelvlcap;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonGainedEvent;
import com.floye.cobblelvlcap.config.CapConfig;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
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

        // Commandes
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
                                            src.sendFeedback(() -> Text.literal("[CobbleLvlCap] Cette commande doit être exécutée par un joueur."), false);
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

        // Abonnement propre à l'événement POKEMON_GAINED (Kotlin Function1) avec priorité NORMAL
        CobblemonEvents.POKEMON_GAINED.subscribe(
                Priority.NORMAL,
                new Function1<PokemonGainedEvent, Unit>() {
                    @Override
                    public Unit invoke(PokemonGainedEvent event) {
                        CaptureHandler.onPokemonGained(event);
                        return Unit.INSTANCE;
                    }
                }
        );
    }

    private static void sendInfo(ServerCommandSource src, ServerPlayerEntity target) {
        int cap = LevelCapService.getCapFor(target);
        CapConfig.CapModel cfg = CapConfig.CFG;

        src.sendFeedback(() -> Text.literal("[CobbleLvlCap] Info pour " + target.getName().getString() + ":"), false);
        src.sendFeedback(() -> Text.literal("  Cap actuel (par tags) : " + cap), false);
        src.sendFeedback(() -> Text.literal("  Cap par défaut : " + cfg.defaultCap), false);

        var tags = target.getCommandTags();
        if (tags.isEmpty()) {
            src.sendFeedback(() -> Text.literal("  Tags : aucun"), false);
        } else {
            src.sendFeedback(() -> Text.literal("  Tags :"), false);
            for (String tag : tags) {
                Integer tCap = cfg.byTag.get(tag);
                if (tCap != null) {
                    src.sendFeedback(() -> Text.literal("    " + tag + " -> " + tCap), false);
                } else {
                    src.sendFeedback(() -> Text.literal("    " + tag + " (pas de cap configuré)"), false);
                }
            }
        }

        src.sendFeedback(() -> Text.literal("  Options :"), false);
        src.sendFeedback(() -> Text.literal("    XP cap : " + cfg.enableExperienceCap), false);
        src.sendFeedback(() -> Text.literal("    Capture: denyCaptureAboveCap : " + cfg.denyCaptureAboveCap), false);
        src.sendFeedback(() -> Text.literal("    Capture: usePartyHighestAsCaptureCap : " + cfg.usePartyHighestAsCaptureCap), false);
        src.sendFeedback(() -> Text.literal("    PC -> équipe: denyPcToPartyAboveCap : " + cfg.denyPcToPartyAboveCap), false);
        src.sendFeedback(() -> Text.literal("    Trade cap : " + cfg.enableTradeCap), false);
        src.sendFeedback(() -> Text.literal("    Bypass commandes : " + cfg.allowCommandBypass), false);
    }
}