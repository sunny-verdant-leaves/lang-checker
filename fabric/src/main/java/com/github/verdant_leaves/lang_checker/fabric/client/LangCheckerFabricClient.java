package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LangChecker.init();

        // 退出游戏时自动保存
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TranslationCollector.scanAndExport());

        // 注册命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("langchecker")
                .then(ClientCommandManager.literal("dump")
                    .executes(context -> {
                        int count = TranslationCollector.scanAndExport();
                        if (count > 0) {
                            context.getSource().sendFeedback(Component.literal("§a[Lang Checker] 导出成功！共捕获 " + count + " 条缺失翻译。"));
                        } else if (count == 0) {
                            context.getSource().sendFeedback(Component.literal("§e[Lang Checker] 当前语言没有缺失翻译。"));
                        } else {
                            context.getSource().sendFeedback(Component.literal("§c[Lang Checker] 导出失败，请查看日志！"));
                        }
                        return 1;
                    })
                )
            );
        });
    }
}