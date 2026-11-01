package com.github.verdant_leaves.lang_checker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TranslationCollector {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 执行全量扫描
    public static int scanAndExport() {
        Minecraft client = Minecraft.getInstance();
        String currentLang = client.options.languageCode;
        String fallbackLang = "en_us"; // 以英文为基准

        // 1. 扫描获取所有英文键值对（这是全量字典）
        Map<String, String> enKeys = new HashMap<>();
        scanLanguageFiles(fallbackLang, enKeys, true);

        // 2. 扫描获取当前语言的所有键
        Map<String, String> currentKeys = new HashMap<>();
        scanLanguageFiles(currentLang, currentKeys, false);

        // 3. 开始对比差集
        Map<String, String> missing = new HashMap<>();
        for (Map.Entry<String, String> entry : enKeys.entrySet()) {
            if (!currentKeys.containsKey(entry.getKey())) {
                // 英文里有，但当前语言没有 -> 缺失！
                missing.put(entry.getKey(), entry.getValue());
            }
        }

        if (missing.isEmpty()) {
            return 0;
        }

        // 4. 写入文件
        Path outputDir = client.gameDirectory.toPath().resolve("missing-translations");
        Path file = outputDir.resolve(currentLang + ".json");
        
        try {
            Files.createDirectories(outputDir);
            // 直接覆盖旧文件（因为是全量扫描）
            Files.writeString(file, GSON.toJson(missing));
            LangChecker.LOGGER.info("Exported {} missing translations to {}", missing.size(), file.toAbsolutePath().toString());
            return missing.size();
        } catch (IOException e) {
            LangChecker.LOGGER.error("Failed to write missing translations", e);
            return -1;
        }
    }

    // 辅助方法：读取所有模组的语言文件
    private static void scanLanguageFiles(String lang, Map<String, String> outputMap, boolean needValues) {
        Minecraft client = Minecraft.getInstance();
        // 搜索所有模组中 lang/zh_cn.json 或 lang/en_us.json 的文件
        Map<ResourceLocation, Resource> resources = client.getResourceManager().listResources(
                "lang", 
                path -> path.getPath().endsWith(lang + ".json")
        );
        
        for (Resource resource : resources.values()) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                Map<?, ?> map = GSON.fromJson(reader, Map.class);
                if (map != null) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (needValues) {
                            outputMap.put(entry.getKey().toString(), entry.getValue().toString());
                        } else {
                            // 如果不需要值，放个空字符串占位就行
                            outputMap.put(entry.getKey().toString(), "");
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}