package com.github.verdant_leaves.lang_checker.fabric.client;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LangChecker.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            TranslationCollector.loadLocalKeys();
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(LangChecker.MOD_ID, "lang_loader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                TranslationCollector.loadLocalKeys();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            TranslationCollector.flush();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("langchecker")
                .then(ClientCommandManager.literal("dump")
                    .executes(context -> {
                        int count = TranslationCollector.flush();
                        if (count > 0) {
                            context.getSource().sendFeedback(Component.literal("§a[Lang Checker] 导出成功！共保存了 " + count + " 条缺失翻译。"));
                        } else if (count == 0) {
                            context.getSource().sendFeedback(Component.literal("§e[Lang Checker] 导出取消：当前没有收集到任何缺失翻译。"));
                        } else {
                            context.getSource().sendFeedback(Component.literal("§c[Lang Checker] 导出发生错误，请查看游戏日志！"));
                        }
                        return 1;
                    })
                )
            );
        });
    }
}