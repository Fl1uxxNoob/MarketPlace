package net.fliuxx.marktPlace;

import net.fliuxx.marktPlace.commands.*;
import net.fliuxx.marktPlace.database.MongoManager;
import net.fliuxx.marktPlace.listeners.InventoryListener;
import net.fliuxx.marktPlace.managers.BlackMarketManager;
import net.fliuxx.marktPlace.managers.EconomyManager;
import net.fliuxx.marktPlace.utils.ConfigManager;
import net.fliuxx.marktPlace.utils.DiscordWebhook;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MarketPlace Plugin Main Class
 * Author: Fl1uxxNoob
 * Version: 1.21.4
 */
public class MarktPlace extends JavaPlugin {

    private static MarktPlace instance;
    private MongoManager mongoManager;
    private EconomyManager economyManager;
    private BlackMarketManager blackMarketManager;
    private ConfigManager configManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        
        // Save default configuration files
        saveDefaultConfig();
        configManager.saveDefaultConfigs();
        
        // Initialize economy manager
        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize MongoDB manager
        mongoManager = new MongoManager(this);
        if (!mongoManager.connect()) {
            getLogger().severe("Failed to connect to MongoDB! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize Discord webhook
        discordWebhook = new DiscordWebhook(this);
        
        // Initialize black market manager
        blackMarketManager = new BlackMarketManager(this);
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start black market refresh task
        blackMarketManager.startRefreshTask();
        
        getLogger().info("MarketPlace plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (mongoManager != null) {
            mongoManager.disconnect();
        }
        
        if (blackMarketManager != null) {
            blackMarketManager.stopRefreshTask();
        }
        
        getLogger().info("MarketPlace plugin has been disabled!");
    }

    private void registerCommands() {
        // Register marketplace command
        MarketPlaceCommand marketPlaceCommand = new MarketPlaceCommand(this);
        getCommand("marketplace").setExecutor(marketPlaceCommand);
        getCommand("marketplace").setTabCompleter(marketPlaceCommand);
        
        // Register sell command
        SellCommand sellCommand = new SellCommand(this);
        getCommand("sell").setExecutor(sellCommand);
        getCommand("sell").setTabCompleter(sellCommand);
        
        // Register black market command
        BlackMarketCommand blackMarketCommand = new BlackMarketCommand(this);
        getCommand("blackmarket").setExecutor(blackMarketCommand);
        getCommand("blackmarket").setTabCompleter(blackMarketCommand);
        
        // Register transactions command
        TransactionsCommand transactionsCommand = new TransactionsCommand(this);
        getCommand("transactions").setExecutor(transactionsCommand);
        getCommand("transactions").setTabCompleter(transactionsCommand);
        
        // Register black market refresh command
        getCommand("blackmarketrefresh").setExecutor(blackMarketCommand);
        getCommand("blackmarketrefresh").setTabCompleter(blackMarketCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    // Getters for managers
    public static MarktPlace getInstance() {
        return instance;
    }

    public MongoManager getMongoManager() {
        return mongoManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BlackMarketManager getBlackMarketManager() {
        return blackMarketManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    /**
     * Reload the plugin configuration
     */
    public void reloadPluginConfig() {
        reloadConfig();
        configManager.reloadConfigs();
        discordWebhook.reloadConfig();
        
        // Reconnect to MongoDB if settings changed
        mongoManager.disconnect();
        if (!mongoManager.connect()) {
            getLogger().severe("Failed to reconnect to MongoDB after config reload!");
        }
    }
}
