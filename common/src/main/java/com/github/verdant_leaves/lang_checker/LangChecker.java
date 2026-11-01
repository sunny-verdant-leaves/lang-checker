package com.github.verdant_leaves.lang_checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangChecker {
    public static final String MOD_ID = "lang_checker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Lang Checker 核心逻辑初始化！");
    }
}