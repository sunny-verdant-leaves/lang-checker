package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.minecraft.network.chat.MutableComponent;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LangChecker.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TranslationCollector.scanAndExport("mix", true));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("langchecker")
                .then(ClientCommandManager.literal("dump")
                    .executes(ctx -> executeDump(ctx.getSource(), "mix", true))
                    .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                        .executes(ctx -> executeDump(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), true))
                        .then(ClientCommandManager.argument("sorted", BoolArgumentType.bool())
                            .executes(ctx -> executeDump(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), BoolArgumentType.getBool(ctx, "sorted"))))
                    )
                )
            );
        });
    }

    private int executeDump(FabricClientCommandSource source, String mode, boolean sorted) {
        int count = TranslationCollector.scanAndExport(mode, sorted);
        
        if (count > 0) {
            String modeStr = switch (mode.toLowerCase()) {
                case "mix", "default" -> Component.translatable("commands.lang_checker.mode.mix").getString();
                case "all" -> Component.translatable("commands.lang_checker.mode.all").getString();
                default -> Component.translatable("commands.lang_checker.mode.custom", mode).getString();
            };
            String suffix = sorted ? Component.translatable("commands.lang_checker.suffix.sorted").getString() : "";

            MutableComponent line1 = Component.translatable("commands.lang_checker.dump.success", count, modeStr + suffix)
                    .withStyle(ChatFormatting.GREEN);
            source.sendFeedback(line1);

            String rawPath = source.getClient().gameDirectory.toPath().resolve("lang_checker").toAbsolutePath().toString();
            String safePath = rawPath.replace("\\", "/");
            
            MutableComponent line2 = Component.translatable("commands.lang_checker.dump.path", rawPath)
                    .withStyle(style -> style
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, safePath))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.lang_checker.dump.path_hover")))
                    );
            
            source.sendFeedback(line2);

        } else if (count == 0) {
            source.sendFeedback(Component.translatable("commands.lang_checker.dump.empty").withStyle(ChatFormatting.YELLOW));
        } else {
            source.sendFeedback(Component.translatable("commands.lang_checker.dump.fail").withStyle(ChatFormatting.RED));
        }
        return 1;
    }
}