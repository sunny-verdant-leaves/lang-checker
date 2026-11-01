package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LangChecker.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("langchecker")
                .then(ClientCommandManager.literal("dump")
                    .executes(context -> {
                        // 触发全量扫描
                        int count = TranslationCollector.scanAndExport();
                        if (count > 0) {
                            context.getSource().sendFeedback(Component.literal("§a[Lang Checker] 扫描完成！共发现 " + count + " 条缺失翻译。"));
                        } else if (count == 0) {
                            context.getSource().sendFeedback(Component.literal("§e[Lang Checker] 扫描完成：当前语言没有缺失翻译，或缺少英文基准。"));
                        } else {
                            context.getSource().sendFeedback(Component.literal("§c[Lang Checker] 扫描或写入时发生错误！"));
                        }
                        return 1;
                    })
                )
            );
        });
    }
}