package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LangChecker.MOD_ID)
public class LangCheckerForge {
    public LangCheckerForge() {
        LangChecker.init();
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                TranslationCollector.loadLocalKeys();
            }
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("langchecker")
            .then(Commands.literal("dump")
                .executes(context -> {
                    int count = TranslationCollector.flush();
                    if (count > 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§a[Lang Checker] 导出成功！共保存了 " + count + " 条缺失翻译。"), false);
                    } else if (count == 0) {
                        context.getSource().sendSuccess(() -> Component.literal("§e[Lang Checker] 导出取消：当前没有收集到任何缺失翻译。"), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal("§c[Lang Checker] 导出发生错误，请查看游戏日志！"), false);
                    }
                    return 1;
                })
            )
        );
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        TranslationCollector.flush();
    }
}