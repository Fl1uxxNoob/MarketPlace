package net.fliuxx.marktPlace.utils;

import net.fliuxx.marktPlace.MarktPlace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Manager for MarketPlace Plugin
 * Handles loading and managing multiple configuration files
 */
public class ConfigManager {

    private final MarktPlace plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(MarktPlace plugin) {
        this.plugin = plugin;
    }

    /**
     * Save default configuration files
     */
    public void saveDefaultConfigs() {
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("guis.yml");
    }

    /**
     * Reload all configuration files
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        
        for (String fileName : configFiles.keySet()) {
            reloadConfig(fileName);
        }
    }

    /**
     * Get a configuration file
     */
    public FileConfiguration getConfig(String fileName) {
        if (!configs.containsKey(fileName)) {
            loadConfig(fileName);
        }
        return configs.get(fileName);
    }

    /**
     * Save a configuration file
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File configFile = configFiles.get(fileName);
        
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save config file " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Reload a specific configuration file
     */
    public void reloadConfig(String fileName) {
        File configFile = configFiles.get(fileName);
        if (configFile != null) {
            configs.put(fileName, YamlConfiguration.loadConfiguration(configFile));
            
            // Look for defaults in the jar
            InputStream defConfigStream = plugin.getResource(fileName);
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
                configs.get(fileName).setDefaults(defConfig);
            }
        }
    }

    /**
     * Load a configuration file
     */
    private void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        configFiles.put(fileName, configFile);
        
        if (!configFile.exists()) {
            saveDefaultConfig(fileName);
        }
        
        configs.put(fileName, YamlConfiguration.loadConfiguration(configFile));
        
        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            configs.get(fileName).setDefaults(defConfig);
        }
    }

    /**
     * Save a default configuration file from the jar
     */
    private void saveDefaultConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    /**
     * Get a message from messages.yml with color codes translated
     */
    public String getMessage(String path) {
        return translateColorCodes(getConfig("messages.yml").getString(path, "Message not found: " + path));
    }

    /**
     * Get a message with placeholders replaced
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        
        return message;
    }

    /**
     * Translate color codes in a string
     */
    private String translateColorCodes(String message) {
        return message.replace('&', '§');
    }

    /**
     * Get GUI configuration
     */
    public FileConfiguration getGuiConfig() {
        return getConfig("guis.yml");
    }

    /**
     * Get messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }
}
