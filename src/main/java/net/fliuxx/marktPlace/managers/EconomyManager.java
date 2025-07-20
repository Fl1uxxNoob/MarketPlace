package net.fliuxx.marktPlace.managers;

import net.fliuxx.marktPlace.MarktPlace;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;

/**
 * Economy Manager
 * Handles all economy-related operations using Vault
 */
public class EconomyManager {

    private final MarktPlace plugin;
    private Economy economy;
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
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found! Please install Vault to use economy features.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy plugin found! Please install an economy plugin like EssentialsX.");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Economy successfully linked with " + economy.getName());
        return true;
    }

    /**
     * Get player's balance
     */
    public double getBalance(Player player) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not initialized! Please check Vault setup.");
            return 0.0;
        }
        return economy.getBalance(player);
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
        if (economy == null) {
            plugin.getLogger().warning("Economy not initialized! Cannot withdraw money.");
            return false;
        }
        
        if (!isValidAmount(amount)) {
            return false;
        }
        
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Failed to withdraw " + formatMoney(amount) + " from " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Deposit money to player
     */
    public boolean deposit(Player player, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not initialized! Cannot deposit money.");
            return false;
        }
        
        if (!isValidAmount(amount)) {
            return false;
        }
        
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Failed to deposit " + formatMoney(amount) + " to " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
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
        if (economy == null) {
            return "Money";
        }
        return economy.currencyNamePlural();
    }

    /**
     * Get currency name singular
     */
    public String getCurrencyNameSingular() {
        if (economy == null) {
            return "Money";
        }
        return economy.currencyNameSingular();
    }

    /**
     * Check if economy is enabled
     */
    public boolean isEnabled() {
        return economy != null && economy.isEnabled();
    }

    /**
     * Transfer money between players
     */
    public boolean transfer(Player from, Player to, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not initialized! Cannot transfer money.");
            return false;
        }
        
        if (!isValidAmount(amount)) {
            return false;
        }
        
        if (!hasEnough(from, amount)) {
            return false;
        }
        
        // First withdraw from sender
        if (!withdraw(from, amount)) {
            return false;
        }
        
        // Then deposit to receiver
        if (!deposit(to, amount)) {
            // Rollback if deposit fails
            deposit(from, amount);
            plugin.getLogger().warning("Transfer failed: Could not deposit to " + to.getName() + ". Money returned to " + from.getName());
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

    /**
     * Check if player has an account
     */
    public boolean hasAccount(Player player) {
        if (economy == null) {
            return false;
        }
        return economy.hasAccount(player);
    }

    /**
     * Create account for player if needed
     */
    public boolean createAccount(Player player) {
        if (economy == null) {
            return false;
        }
        
        if (hasAccount(player)) {
            return true; // Account already exists
        }
        
        return economy.createPlayerAccount(player);
    }

    /**
     * Get economy provider name
     */
    public String getEconomyProviderName() {
        if (economy == null) {
            return "None";
        }
        return economy.getName();
    }

    /**
     * Check if economy supports banks
     */
    public boolean hasBankSupport() {
        if (economy == null) {
            return false;
        }
        return economy.hasBankSupport();
    }

    /**
     * Get minimum allowed transaction amount
     */
    public double getMinimumAmount() {
        return plugin.getConfig().getDouble("economy.minimum-amount", 0.01);
    }

    /**
     * Get maximum allowed transaction amount
     */
    public double getMaximumAmount() {
        return plugin.getConfig().getDouble("economy.maximum-amount", 1000000.0);
    }

    /**
     * Check if amount is within allowed range
     */
    public boolean isAmountInRange(double amount) {
        return amount >= getMinimumAmount() && amount <= getMaximumAmount();
    }

    /**
     * Validate transaction amount
     */
    public boolean isValidTransaction(double amount) {
        return isValidAmount(amount) && isAmountInRange(amount);
    }
}
