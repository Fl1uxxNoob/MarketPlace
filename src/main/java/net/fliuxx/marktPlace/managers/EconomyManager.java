package net.fliuxx.marktPlace.managers;

import net.fliuxx.marktPlace.MarktPlace;
// import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
// import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;

/**
 * Economy Manager
 * Handles all economy-related operations using Vault
 */
public class EconomyManager {

    private final MarktPlace plugin;
    // private Economy economy;
    private final DecimalFormat decimalFormat;

    public EconomyManager(MarktPlace plugin) {
        this.plugin = plugin;
        
        // Initialize decimal format based on config
        int decimalPlaces = plugin.getConfig().getInt("economy.decimal-places", 2);
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            for (int i = 0; i < decimalPlaces; i++) {
                pattern.append("0");
            }
        }
        this.decimalFormat = new DecimalFormat(pattern.toString());
    }

    /**
     * Setup economy using Vault
     */
    public boolean setupEconomy() {
        // TODO: Re-enable Vault integration when dependencies are resolved
        plugin.getLogger().info("Economy manager initialized (Vault integration disabled for now)");
        return true;
        
        /*
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy plugin found!");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Economy successfully linked with " + economy.getName());
        return true;
        */
    }

    /**
     * Get player's balance
     */
    public double getBalance(Player player) {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return 1000.0; // Temporary placeholder for testing
    }

    /**
     * Check if player has enough money
     */
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Withdraw money from player
     */
    public boolean withdraw(Player player, double amount) {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return hasEnough(player, amount);
    }

    /**
     * Deposit money to player
     */
    public boolean deposit(Player player, double amount) {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return true;
    }

    /**
     * Format money amount
     */
    public String formatMoney(double amount) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        return symbol + decimalFormat.format(amount);
    }

    /**
     * Parse money amount from string
     */
    public double parseMoney(String input) {
        try {
            // Remove currency symbol if present
            String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
            if (input.startsWith(symbol)) {
                input = input.substring(symbol.length());
            }
            
            // Remove commas and parse
            input = input.replace(",", "");
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1.0; // Invalid amount
        }
    }

    /**
     * Check if amount is valid
     */
    public boolean isValidAmount(double amount) {
        return amount > 0 && amount <= Double.MAX_VALUE && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    /**
     * Get currency name
     */
    public String getCurrencyName() {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return "Money";
    }

    /**
     * Get currency name singular
     */
    public String getCurrencyNameSingular() {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return "Money";
    }

    /**
     * Check if economy is enabled
     */
    public boolean isEnabled() {
        // TODO: Re-enable Vault integration when dependencies are resolved
        return true;
    }

    /**
     * Transfer money between players
     */
    public boolean transfer(Player from, Player to, double amount) {
        if (!withdraw(from, amount)) {
            return false;
        }
        
        if (!deposit(to, amount)) {
            // Rollback if deposit fails
            deposit(from, amount);
            return false;
        }
        
        return true;
    }

    /**
     * Get formatted balance
     */
    public String getFormattedBalance(Player player) {
        return formatMoney(getBalance(player));
    }

    /**
     * Round money to configured decimal places
     */
    public double roundMoney(double amount) {
        int decimalPlaces = plugin.getConfig().getInt("economy.decimal-places", 2);
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(amount * factor) / factor;
    }
}
