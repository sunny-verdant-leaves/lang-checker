package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
            source.sendSuccess(() -> Component.literal("§a[Lang Checker] 成功导出 " + count + " 条缺失翻译 (模式: " + mode + ")"), false);
        } else if (count == 0) {
            source.sendSuccess(() -> Component.literal("§e[Lang Checker] 未发现缺失翻译。"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§c[Lang Checker] 导出失败，请查看日志！"), false);
        }
        return 1;
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        TranslationCollector.scanAndExport("mix");
    }
}