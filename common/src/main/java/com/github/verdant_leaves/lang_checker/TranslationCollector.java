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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationCollector {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> capturedComponents = new ConcurrentHashMap<>();
    
    // 记录每个键真正的归属模组（从 lang 文件所在的 namespace 获得）
    private static final Map<String, String> keyToNamespace = new ConcurrentHashMap<>();

    public static void recordRawComponent(String key, String fallback) {
        if (key == null || key.isEmpty()) return;
        String value = (fallback != null && !fallback.isEmpty()) ? fallback : key;
        capturedComponents.putIfAbsent(key, value);
    }

    public static int scanAndExport(String mode) {
        Minecraft client = Minecraft.getInstance();
        String currentLang = client.options.languageCode;
        String fallbackLang = "en_us";
        String targetModId = "";

        // 解析模式
        if (!mode.equalsIgnoreCase("mix") && !mode.equalsIgnoreCase("default") && !mode.equalsIgnoreCase("all")) {
            targetModId = mode.toLowerCase();
        }

        Map<String, String> enKeys = new HashMap<>();
        scanLanguageFiles(fallbackLang, enKeys);

        Map<String, String> currentKeys = new HashMap<>();
        scanLanguageFiles(currentLang, currentKeys);

        Map<String, String> missing = new HashMap<>();
        
        // 1. 主动比对
        for (Map.Entry<String, String> entry : enKeys.entrySet()) {
            String key = entry.getKey();
            if (!currentKeys.containsKey(key)) {
                String modid = keyToNamespace.getOrDefault(key, "minecraft");
                if (targetModId.isEmpty() || modid.equalsIgnoreCase(targetModId)) {
                    missing.put(key, entry.getValue());
                }
            }
        }

        // 2. 被动拦截
        for (Map.Entry<String, String> entry : capturedComponents.entrySet()) {
            String key = entry.getKey();
            if (!currentKeys.containsKey(key)) {
                String modid = keyToNamespace.getOrDefault(key, "unknown");
                if (targetModId.isEmpty() || modid.equalsIgnoreCase(targetModId)) {
                    missing.putIfAbsent(key, entry.getValue());
                }
            }
        }

        if (missing.isEmpty()) return 0;

        // 输出根目录: <游戏根目录>/lang_checker
        Path baseDir = client.gameDirectory.toPath().resolve("lang_checker");
        int exportedCount = 0;

        try {
            if (mode.equalsIgnoreCase("mix") || mode.equalsIgnoreCase("default")) {
                Path file = baseDir.resolve(currentLang + ".json");
                Files.createDirectories(baseDir);
                Files.writeString(file, GSON.toJson(missing));
                exportedCount = missing.size();
            } else if (mode.equalsIgnoreCase("all")) {
                // 按照真实的 namespace 分组
                Map<String, Map<String, String>> grouped = new HashMap<>();
                for (Map.Entry<String, String> entry : missing.entrySet()) {
                    String modid = keyToNamespace.getOrDefault(entry.getKey(), "unknown");
                    grouped.computeIfAbsent(modid, k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
                }

                for (Map.Entry<String, Map<String, String>> entry : grouped.entrySet()) {
                    Path file = baseDir.resolve(entry.getKey()).resolve(currentLang + ".json");
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, GSON.toJson(entry.getValue()));
                    exportedCount += entry.getValue().size();
                }
            } else {
                // 指定模组模式
                Path file = baseDir.resolve(targetModId).resolve(currentLang + ".json");
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(missing));
                exportedCount = missing.size();
            }

            if (exportedCount > 0) {
                LangChecker.LOGGER.info("Exported {} missing translations (Mode: {}) to {}", exportedCount, mode, baseDir.toAbsolutePath());
                capturedComponents.clear();
            }
            return exportedCount;

        } catch (IOException e) {
            LangChecker.LOGGER.error("Failed to write missing translations", e);
            return -1;
        }
    }

    private static void scanLanguageFiles(String lang, Map<String, String> outputMap) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        
        // 遍历所有资源包的 lang 目录
        resourceManager.listResourceStacks("lang", path -> path.getPath().endsWith(lang + ".json"))
            .forEach((location, resources) -> {
                // 这里的 location.getNamespace() 就是如假包换的模组 ID！
                String namespace = location.getNamespace();
                
                for (Resource resource : resources) {
                    try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                        Map<?, ?> map = GSON.fromJson(reader, Map.class);
                        if (map != null) {
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                String key = entry.getKey().toString();
                                outputMap.putIfAbsent(key, entry.getValue().toString());
                                // 记录该键属于哪个模组
                                keyToNamespace.putIfAbsent(key, namespace);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
    }
}