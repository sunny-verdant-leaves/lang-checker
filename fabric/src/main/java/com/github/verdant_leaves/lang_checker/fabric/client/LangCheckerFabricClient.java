package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LangChecker.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TranslationCollector.scanAndExport("mix"));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("langchecker")
                .then(ClientCommandManager.literal("dump")
                    .executes(ctx -> executeDump(ctx.getSource(), "mix"))
                    .then(ClientCommandManager.literal("default")
                        .executes(ctx -> executeDump(ctx.getSource(), "mix")))
                    .then(ClientCommandManager.literal("mix")
                        .executes(ctx -> executeDump(ctx.getSource(), "mix")))
                    .then(ClientCommandManager.literal("all")
                        .executes(ctx -> executeDump(ctx.getSource(), "all")))
                    .then(ClientCommandManager.argument("modid", StringArgumentType.word())
                        .executes(ctx -> executeDump(ctx.getSource(), StringArgumentType.getString(ctx, "modid"))))
                )
            );
        });
    }

    private int executeDump(FabricClientCommandSource source, String mode) {
        int count = TranslationCollector.scanAndExport(mode);
        if (count > 0) {
            source.sendFeedback(Component.literal("§a[Lang Checker] 成功导出 " + count + " 条缺失翻译 (模式: " + mode + ")"));
        } else if (count == 0) {
            source.sendFeedback(Component.literal("§e[Lang Checker] 未发现缺失翻译。"));
        } else {
            source.sendFeedback(Component.literal("§c[Lang Checker] 导出失败，请查看日志！"));
        }
        return 1;
    }
}