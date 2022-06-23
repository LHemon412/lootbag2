package com.github.lhemon412.lootbag2;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class LanguageManager {
    private final Lootbag2Plugin plugin;
    private String activeLang;
    private final Map<String, Map<String, String>> langMap = new HashMap<>();
    private final Map<String, Map<String, List<String>>> langListMap = new HashMap<>();

    public LanguageManager(Lootbag2Plugin instance, String activeLang) {
        plugin = instance;
        this.activeLang = activeLang;
        load();
    }

    public boolean reload() {
        unload();
        return load();
    }

    public boolean load() {
        File langSaveDir = new File(plugin.getDataFolder(), "languages");

        if (!langSaveDir.exists()) {
            plugin.saveResource("languages/zh_TW.yml", true);
            plugin.saveResource("languages/en.yml", true);
        }

        for (File langFile : Objects.requireNonNull(langSaveDir.listFiles())) {
            FileConfiguration langConfig = new YamlConfiguration();
            String langID = langFile.getName().replaceFirst("[.][^.]+$", "");
            try {
                langConfig.load(langFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                plugin.getLogger().severe("Error while loading saved lootbag files.");
                return false;
            }
            Map<String, String> langContent = new HashMap<>();
            Map<String, List<String>> langListContent = new HashMap<>();
            for (String category : langConfig.getKeys(false)) {
                for (String item : Objects.requireNonNull(langConfig.getConfigurationSection(category)).getKeys(false)) {
                    String path = category + "." + item;
                    if (langConfig.isList(path)) {
                        List<String> rawList = new ArrayList<>();
                        langConfig.getStringList(path).forEach(s -> rawList.add(s.replaceAll("&", "§")));
                        langListContent.put(path, rawList);
                    } else {
                        langContent.put(path, Objects.requireNonNull(langConfig.getString(path)).replaceAll("&", "§"));
                    }
                }
            }
            langMap.put(langID, langContent);
            langListMap.put(langID, langListContent);
        }

        if (!langMap.containsKey(activeLang)) {
            plugin.getLogger().severe("Language file '" + activeLang + ".yml' does not exist.");
            plugin.getLogger().severe("Using default 'en.yml'.");
            activeLang = "en";
        }

        return true;
    }

    public void unload() {
        langMap.clear();
    }

    public void setLang(String langId) { activeLang = langId; }

    public String getPrefix() {
        return langMap.get(activeLang).getOrDefault("System.PREFIX", "");
    }

    public String get(String key) {
        String msg = langMap.get(activeLang).get(key);
        if (msg == null) return "LANG FILE MISSING KEY : " + key;
        else return msg;
    }

    public List<String> getList(String key) {
        List<String> msgList = langListMap.get(activeLang).get(key);
        if (msgList == null) return Collections.singletonList("LANG FILE MISSING KEY : " + key);
        else return msgList;
    }

    public List<String> getPrefixedList(String key) {
        List<String> msgList = new ArrayList<>();
        getList(key).forEach(s -> msgList.add(getPrefix() + s));
        return msgList;
    }

    public String getPrefixed(String key) {
        String msg = langMap.get(activeLang).get(key);
        if (msg == null) return "LANG FILE MISSING KEY : " + key;
        else return getPrefix() + msg;
    }
}
