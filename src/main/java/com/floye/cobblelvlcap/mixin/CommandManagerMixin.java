package com.floye.cobblelvlcap.mixin;

import com.floye.cobblelvlcap.CommandContext;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

// Commandes sans préfixe (exécutées via dispatcher)
    @Inject(method = "execute", at = @At("HEAD"))
    private void cobblelvlcap$enterExecute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        CommandContext.enter();
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void cobblelvlcap$exitExecute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        CommandContext.exit();
    }

    // Commandes avec préfixe (ex: console/command blocks)
    @Inject(method = "executeWithPrefix", at = @At("HEAD"))
    private void cobblelvlcap$enterExecutePrefixed(ServerCommandSource source, String command, CallbackInfo ci) {
        CommandContext.enter();
    }

    @Inject(method = "executeWithPrefix", at = @At("RETURN"))
    private void cobblelvlcap$exitExecutePrefixed(ServerCommandSource source, String command, CallbackInfo ci) {
        CommandContext.exit();
    }
}