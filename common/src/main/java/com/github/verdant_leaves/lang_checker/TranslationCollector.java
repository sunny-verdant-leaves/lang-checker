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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationCollector {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> capturedComponents = new ConcurrentHashMap<>();
    private static final Map<String, String> keyToNamespace = new ConcurrentHashMap<>();

    public static void recordRawComponent(String key, String fallback) {
        if (key == null || key.isEmpty()) return;
        String value = (fallback != null && !fallback.isEmpty()) ? fallback : key;
        capturedComponents.putIfAbsent(key, value);
    }

    public static int scanAndExport(String mode, boolean sortKeys) {
        Minecraft client = Minecraft.getInstance();
        String currentLang = client.options.languageCode;
        String fallbackLang = "en_us";
        String targetModId = "";

        if (!mode.equalsIgnoreCase("mix") && !mode.equalsIgnoreCase("default") && !mode.equalsIgnoreCase("all")) {
            targetModId = mode.toLowerCase();
        }

        Map<String, String> enKeys = new HashMap<>();
        scanLanguageFiles(fallbackLang, enKeys);

        Map<String, String> currentKeys = new HashMap<>();
        scanLanguageFiles(currentLang, currentKeys);

        Map<String, String> rawMissing = new HashMap<>();
        
        for (Map.Entry<String, String> entry : enKeys.entrySet()) {
            String key = entry.getKey();
            if (!currentKeys.containsKey(key)) {
                String modid = keyToNamespace.getOrDefault(key, "minecraft");
                if (targetModId.isEmpty() || modid.equalsIgnoreCase(targetModId)) {
                    rawMissing.put(key, entry.getValue());
                }
            }
        }

        for (Map.Entry<String, String> entry : capturedComponents.entrySet()) {
            String key = entry.getKey();
            if (!currentKeys.containsKey(key)) {
                String modid = keyToNamespace.getOrDefault(key, "unknown");
                if (targetModId.isEmpty() || modid.equalsIgnoreCase(targetModId)) {
                    rawMissing.putIfAbsent(key, entry.getValue());
                }
            }
        }

        if (rawMissing.isEmpty()) return 0;

        Map<String, String> missing = sortKeys ? new TreeMap<>(rawMissing) : rawMissing;

        Path baseDir = client.gameDirectory.toPath().resolve("lang_checker");
        int exportedCount = 0;

        try {
            if (mode.equalsIgnoreCase("mix") || mode.equalsIgnoreCase("default")) {
                Path file = baseDir.resolve(currentLang + ".json");
                writeFile(file, missing, sortKeys);
                exportedCount = missing.size();
            } else if (mode.equalsIgnoreCase("all")) {
                Map<String, Map<String, String>> grouped = new HashMap<>();
                for (Map.Entry<String, String> entry : missing.entrySet()) {
                    String modid = keyToNamespace.getOrDefault(entry.getKey(), "unknown");
                    if (sortKeys) {
                        grouped.computeIfAbsent(modid, k -> new TreeMap<>()).put(entry.getKey(), entry.getValue());
                    } else {
                        grouped.computeIfAbsent(modid, k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
                    }
                }

                for (Map.Entry<String, Map<String, String>> entry : grouped.entrySet()) {
                    Path file = baseDir.resolve(entry.getKey()).resolve(currentLang + ".json");
                    writeFile(file, entry.getValue(), sortKeys);
                    exportedCount += entry.getValue().size();
                }
            } else {
                Path file = baseDir.resolve(targetModId).resolve(currentLang + ".json");
                writeFile(file, missing, sortKeys);
                exportedCount = missing.size();
            }

            if (exportedCount > 0) {
                LangChecker.LOGGER.info("Exported {} missing translations (Mode: {}, Sorted: {}) to {}", exportedCount, mode, sortKeys, baseDir.toAbsolutePath());
                capturedComponents.clear();
            }
            return exportedCount;

        } catch (IOException e) {
            LangChecker.LOGGER.error("Failed to write missing translations", e);
            return -1;
        }
    }

    private static void writeFile(Path file, Map<String, String> map, boolean sortKeys) throws IOException {
        Files.createDirectories(file.getParent());
        if (sortKeys) {
            Files.writeString(file, toJsonWithBlankLines(map));
        } else {
            Files.writeString(file, GSON.toJson(map));
        }
    }

    // 自定义 JSON 序列化：按照前缀分组插入空行
    private static String toJsonWithBlankLines(Map<String, String> map) {
        if (map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{\n");
        String lastPrefix = null;
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            int dot = key.indexOf('.');
            String prefix = dot != -1 ? key.substring(0, dot) : key;
            
            // 如果第一段前缀变了，插入一个空行
            if (lastPrefix != null && !lastPrefix.equals(prefix)) {
                sb.append("\n");
            }
            lastPrefix = prefix;
            
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            
            // 利用 GSON 保证字符串转义
            sb.append("  ").append(GSON.toJson(key)).append(": ").append(GSON.toJson(entry.getValue()));
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static void scanLanguageFiles(String lang, Map<String, String> outputMap) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        resourceManager.listResourceStacks("lang", path -> path.getPath().endsWith(lang + ".json"))
            .forEach((location, resources) -> {
                String namespace = location.getNamespace();
                for (Resource resource : resources) {
                    try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                        Map<?, ?> map = GSON.fromJson(reader, Map.class);
                        if (map != null) {
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                String key = entry.getKey().toString();
                                outputMap.putIfAbsent(key, entry.getValue().toString());
                                keyToNamespace.putIfAbsent(key, namespace);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
    }
}