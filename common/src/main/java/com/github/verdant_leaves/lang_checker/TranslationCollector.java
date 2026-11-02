package com.github.verdant_leaves.lang_checker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationCollector {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 存放 Mixin 被动拦截到的硬编码键
    private static final Map<String, String> capturedComponents = new ConcurrentHashMap<>();

    // Mixin 拦截到 TranslatableComponent 时调用
    public static void recordRawComponent(String key, String fallback) {
        if (key == null || key.isEmpty()) return;
        // 把原始的 fallback 存起来，如果没有 fallback 就用 key 顶替
        String value = (fallback != null && !fallback.isEmpty()) ? fallback : key;
        capturedComponents.putIfAbsent(key, value);
    }

    // 命令/事件触发的导出逻辑
    public static int scanAndExport() {
        Minecraft client = Minecraft.getInstance();
        String currentLang = client.options.languageCode;
        String fallbackLang = "en_us";

        Map<String, String> enKeys = new HashMap<>();
        scanLanguageFiles(fallbackLang, enKeys);

        Map<String, String> currentKeys = new HashMap<>();
        scanLanguageFiles(currentLang, currentKeys);

        Map<String, String> missing = new HashMap<>();
        
        // 1. 主动比对：英文有但中文没有的
        for (Map.Entry<String, String> entry : enKeys.entrySet()) {
            if (!currentKeys.containsKey(entry.getKey())) {
                missing.put(entry.getKey(), entry.getValue());
            }
        }

        // 2. 被动拦截：Mixin 抓到的硬编码（过滤掉中文里已经有的）
        for (Map.Entry<String, String> entry : capturedComponents.entrySet()) {
            if (!currentKeys.containsKey(entry.getKey())) {
                missing.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        if (missing.isEmpty()) {
            return 0;
        }

        Path outputDir = client.gameDirectory.toPath().resolve("missing-translations");
        Path file = outputDir.resolve(currentLang + ".json");
        
        try {
            Files.createDirectories(outputDir);
            Files.writeString(file, GSON.toJson(missing));
            LangChecker.LOGGER.info("Exported {} missing translations to {}", missing.size(), file.toAbsolutePath().toString());
            
            // 导出后清空被动收集，避免下次重复
            capturedComponents.clear();
            return missing.size();
        } catch (IOException e) {
            LangChecker.LOGGER.error("Failed to write missing translations", e);
            return -1;
        }
    }

    private static void scanLanguageFiles(String lang, Map<String, String> outputMap) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        
        // 遍历所有资源包里的 lang/xxx.json
        resourceManager.listResourceStacks("lang", path -> path.getPath().endsWith(lang + ".json"))
            .forEach((location, resources) -> {
                for (Resource resource : resources) {
                    try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                        Map<?, ?> map = GSON.fromJson(reader, Map.class);
                        if (map != null) {
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                outputMap.putIfAbsent(entry.getKey().toString(), entry.getValue().toString());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
    }
}