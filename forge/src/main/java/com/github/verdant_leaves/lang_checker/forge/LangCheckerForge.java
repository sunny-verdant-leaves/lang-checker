package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
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
                .executes(context -> {
                    int count = TranslationCollector.scanAndExport();
                    if (count > 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§a[Lang Checker] 扫描完成！共发现 " + count + " 条缺失翻译。"), false);
                    } else if (count == 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§e[Lang Checker] 扫描完成：当前语言没有缺失翻译，或缺少英文基准。"), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal("§c[Lang Checker] 扫描或写入时发生错误！"), false);
                    }
                    return 1;
                })
            )
        );
    }
}