package com.github.verdant_leaves.lang_checker.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangCheckerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoggerFactory.getLogger("LangChecker").info("Lang Checker Fabric 客户端环境测试成功！");
    }
}