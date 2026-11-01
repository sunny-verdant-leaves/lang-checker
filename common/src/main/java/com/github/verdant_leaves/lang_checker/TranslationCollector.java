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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationCollector {
    private static final Map<String, String> collected = new ConcurrentHashMap<>();
    // 线程安全的 Set
    public static final Set<String> availableKeys = ConcurrentHashMap.newKeySet();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadLocalKeys() {
        availableKeys.clear();
        Minecraft client = Minecraft.getInstance();
        String lang = client.options.languageCode;
        
        try {
            // 魔法在此：读取所有命名空间下 lang/zh_cn.json 的文件
            Map<ResourceLocation, Resource> resources = client.getResourceManager().listResources(
                    "lang", 
                    path -> path.getPath().endsWith(lang + ".json")
            );
            
            for (Resource resource : resources.values()) {
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    Map<?, ?> map = GSON.fromJson(reader, Map.class);
                    if (map != null) {
                        for (Object k : map.keySet()) {
                            availableKeys.add(k.toString());
                        }
                    }
                } catch (Exception ignored) {}
            }
            LangChecker.LOGGER.info("Loaded {} local keys for language {}", availableKeys.size(), lang);
        } catch (Exception e) {
            LangChecker.LOGGER.error("Failed to read language files", e);
        }
    }

    public static void record(String key, String value) {
        if (!collected.containsKey(key)) {
            collected.put(key, value);
            LangChecker.LOGGER.info("Missing translation: {} -> {}", key, value);
        }
    }

    public static int flush() {
        int count = collected.size();
        if (count == 0) return 0;
        
        Minecraft client = Minecraft.getInstance();
        String lang = client.options.languageCode;
        Path outputDir = client.gameDirectory.toPath().resolve("missing-translations");
        Path file = outputDir.resolve(lang + ".json");
        
        try {
            Files.createDirectories(outputDir);
            Map<String, String> existing = new HashMap<>();
            if (Files.exists(file)) {
                String content = Files.readString(file);
                Map<?, ?> oldMap = GSON.fromJson(content, Map.class);
                if (oldMap != null) {
                    for (Map.Entry<?, ?> entry : oldMap.entrySet()) {
                        existing.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }
            
            existing.putAll(collected);
            Files.writeString(file, GSON.toJson(existing));
            
            LangChecker.LOGGER.info("Exported {} missing translations to {}", count, file.toAbsolutePath().toString());
            collected.clear();
            return count;
            
        } catch (IOException e) {
            LangChecker.LOGGER.error("Failed to write missing translations", e);
            return -1;
        }
    }
}