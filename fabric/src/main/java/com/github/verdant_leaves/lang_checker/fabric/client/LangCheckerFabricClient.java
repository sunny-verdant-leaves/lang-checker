package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

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
            source.sendFeedback(Component.translatable("commands.lang_checker.dump.success", count, mode));
            // 生成可点击打开的目录链接
            String path = source.getClient().gameDirectory.toPath().resolve("lang_checker").toAbsolutePath().toString();
            Component clickText = Component.translatable("commands.lang_checker.dump.click")
                .withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.lang_checker.dump.hover")))
                );
            source.sendFeedback(clickText);
        } else if (count == 0) {
            source.sendFeedback(Component.translatable("commands.lang_checker.dump.empty"));
        } else {
            source.sendFeedback(Component.translatable("commands.lang_checker.dump.fail"));
        }
        return 1;
    }
}