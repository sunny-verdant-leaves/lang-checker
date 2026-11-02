package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
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

    // 命令注册
    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("langchecker")
            .then(Commands.literal("dump")
                .executes(context -> {
                    int count = TranslationCollector.scanAndExport();
                    if (count > 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§a[Lang Checker] 导出成功！共捕获 " + count + " 条缺失翻译。"), false);
                    } else if (count == 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§e[Lang Checker] 当前语言没有缺失翻译。"), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal("§c[Lang Checker] 导出失败，请查看日志！"), false);
                    }
                    return 1;
                })
            )
        );
    }

    // 退出世界时兜底保存
    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        TranslationCollector.scanAndExport();
    }
}