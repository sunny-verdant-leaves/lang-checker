package com.github.verdant_leaves.lang_checker.forge;

import com.github.verdant_leaves.lang_checker.LangChecker;
import com.github.verdant_leaves.lang_checker.TranslationCollector;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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
            String modeStr;
            if (mode.equalsIgnoreCase("mix") || mode.equalsIgnoreCase("default")) {
                modeStr = Component.translatable("commands.lang_checker.mode.mix").getString();
            } else if (mode.equalsIgnoreCase("all")) {
                modeStr = Component.translatable("commands.lang_checker.mode.all").getString();
            } else {
                modeStr = Component.translatable("commands.lang_checker.mode.custom", mode).getString();
            }

            source.sendSuccess(() -> Component.translatable("commands.lang_checker.dump.success", count, modeStr), false);

            String rawPath = Minecraft.getInstance().gameDirectory.toPath().resolve("lang_checker").toAbsolutePath().toString();
            String safePath = rawPath.replace("\\", "/");
            
            MutableComponent line2 = Component.translatable("commands.lang_checker.dump.path", rawPath)
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, safePath))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.lang_checker.dump.path_hover")))
                    );

            source.sendSuccess(() -> line2, false);

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