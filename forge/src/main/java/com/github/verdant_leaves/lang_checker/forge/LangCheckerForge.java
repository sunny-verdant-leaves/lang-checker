package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(LangChecker.MOD_ID)
public class LangCheckerForge {
    public LangCheckerForge() {
        LangChecker.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("langchecker")
            .then(Commands.literal("dump")
                .executes(ctx -> executeDump(ctx.getSource(), "mix"))
                .then(Commands.literal("default")
                    .executes(ctx -> executeDump(ctx.getSource(), "mix")))
                .then(Commands.literal("mix")
                    .executes(ctx -> executeDump(ctx.getSource(), "mix")))
                .then(Commands.literal("all")
                    .executes(ctx -> executeDump(ctx.getSource(), "all")))
                .then(Commands.argument("modid", StringArgumentType.word())
                    .executes(ctx -> executeDump(ctx.getSource(), StringArgumentType.getString(ctx, "modid"))))
            )
        );
    }

    private int executeDump(CommandSourceStack source, String mode) {
        int count = TranslationCollector.scanAndExport(mode);
        if (count > 0) {
            source.sendSuccess(() -> Component.translatable("commands.lang_checker.dump.success", count, mode), false);
            
            // 获取游戏根目录，拼出 lang_checker 文件夹的绝对路径
            String path = source.getServer().getServerDirectory().toPath().resolve("lang_checker").toAbsolutePath().toString();
            
            // 如果存在版本隔离，可以取客户端目录
            try {
                path = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("lang_checker").toAbsolutePath().toString();
            } catch (Exception ignored) {}

            final String finalPath = path;
            Component clickText = Component.translatable("commands.lang_checker.dump.click")
                .withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, finalPath))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.lang_checker.dump.hover")))
                );
            source.sendSuccess(() -> clickText, false);
        } else if (count == 0) {
            source.sendSuccess(() -> Component.translatable("commands.lang_checker.dump.empty"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.lang_checker.dump.fail"), false);
        }
        return 1;
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        TranslationCollector.scanAndExport("mix");
    }
}