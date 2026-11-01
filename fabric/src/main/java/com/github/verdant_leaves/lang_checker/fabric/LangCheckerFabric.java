package com.github.verdant_leaves.lang_checker.fabric;

import com.github.verdant_leaves.lang_checker.LangChecker;
import net.fabricmc.api.ModInitializer;

public class LangCheckerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LangChecker.init();
    }
}