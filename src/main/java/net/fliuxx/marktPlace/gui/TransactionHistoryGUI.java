package net.fliuxx.marktPlace.gui;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction History GUI
 * Handles the transaction history interface
 */
public class TransactionHistoryGUI {

    private final MarktPlace plugin;
    private final Player player;
    private final List<Transaction> transactions;
    private final int itemsPerPage;
    private int currentPage;
    private Inventory inventory;

    public TransactionHistoryGUI(MarktPlace plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.transactions = plugin.getMongoManager().getTransactionsByPlayer(player.getUniqueId());
        this.itemsPerPage = 45; // 9x5 grid for items
        this.currentPage = 0;
        
        createInventory();
    }

    /**
     * Create the inventory
     */
    private void createInventory() {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("transactions");
        
        String title = guiConfig.getString("title", "&9Transaction History - Page {page}");
        title = title.replace("{page}", String.valueOf(currentPage + 1));
        title = title.replace('&', '§');
        
        int rows = guiConfig.getInt("rows", 6);
        
        inventory = Bukkit.createInventory(null, rows * 9, title);
        
        populateInventory();
    }

    /**
     * Populate the inventory with items
     */
    private void populateInventory() {
        inventory.clear();
        
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("transactions");
        
        // Add navigation items
        addNavigationItems(guiConfig);
        
        // Add filler items
        addFillerItems(guiConfig);
        
        // Add transaction items
        addTransactionItems(guiConfig);
        
        // Add page info
        addPageInfo(guiConfig);
    }

    /**
     * Add navigation items to inventory
     */
    private void addNavigationItems(ConfigurationSection guiConfig) {
        // Next page button
        if (hasNextPage()) {
            ConfigurationSection nextPageConfig = guiConfig.getConfigurationSection("items.next-page");
            if (nextPageConfig != null) {
                ItemStack nextPage = createGuiItem(nextPageConfig);
                inventory.setItem(nextPageConfig.getInt("slot", 53), nextPage);
            }
        }
        
        // Previous page button
        if (hasPreviousPage()) {
            ConfigurationSection prevPageConfig = guiConfig.getConfigurationSection("items.previous-page");
            if (prevPageConfig != null) {
                ItemStack prevPage = createGuiItem(prevPageConfig);
                inventory.setItem(prevPageConfig.getInt("slot", 45), prevPage);
            }
        }
        
        // Close button
        ConfigurationSection closeConfig = guiConfig.getConfigurationSection("items.close");
        if (closeConfig != null) {
            ItemStack close = createGuiItem(closeConfig);
            inventory.setItem(closeConfig.getInt("slot", 49), close);
        }
    }

    /**
     * Add filler items to inventory
     */
    private void addFillerItems(ConfigurationSection guiConfig) {
        ConfigurationSection fillerConfig = guiConfig.getConfigurationSection("items.filler");
        if (fillerConfig != null && fillerConfig.getBoolean("enabled", true)) {
            List<Integer> slots = fillerConfig.getIntegerList("slots");
            ItemStack filler = createGuiItem(fillerConfig);
            
            for (int slot : slots) {
                inventory.setItem(slot, filler);
            }
        }
    }

    /**
     * Add transaction items to inventory
     */
    private void addTransactionItems(ConfigurationSection guiConfig) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, transactions.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            // Skip navigation slots
            while (isNavigationSlot(slot)) {
                slot++;
            }
            
            if (slot >= 45) break; // Don't go beyond item area
            
            Transaction transaction = transactions.get(i);
            ItemStack displayItem = createTransactionDisplay(transaction, guiConfig);
            
            inventory.setItem(slot, displayItem);
            slot++;
        }
    }

    /**
     * Add page information
     */
    private void addPageInfo(ConfigurationSection guiConfig) {
        ConfigurationSection pageInfoConfig = guiConfig.getConfigurationSection("items.page-info");
        if (pageInfoConfig != null) {
            ItemStack pageInfo = createGuiItem(pageInfoConfig);
            ItemMeta meta = pageInfo.getItemMeta();
            
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : lore) {
                        line = line.replace("{current}", String.valueOf(currentPage + 1));
                        line = line.replace("{total}", String.valueOf(getTotalPages()));
                        newLore.add(line);
                    }
                    meta.setLore(newLore);
                }
                
                pageInfo.setItemMeta(meta);
            }
            
            inventory.setItem(pageInfoConfig.getInt("slot", 51), pageInfo);
        }
    }

    /**
     * Create a transaction display item
     */
    private ItemStack createTransactionDisplay(Transaction transaction, ConfigurationSection guiConfig) {
        ConfigurationSection displayConfig = guiConfig.getConfigurationSection("transaction-display");
        if (displayConfig == null) {
            return new ItemStack(Material.PAPER);
        }
        
        ConfigurationSection typeConfig = null;
        boolean isBuyer = transaction.getBuyerId().equals(player.getUniqueId());
        boolean isBlackMarket = transaction.getType() == Transaction.TransactionType.BLACK_MARKET;
        
        if (isBuyer) {
            typeConfig = isBlackMarket ? 
                displayConfig.getConfigurationSection("blackmarket-bought") :
                displayConfig.getConfigurationSection("bought");
        } else {
            typeConfig = isBlackMarket ? 
                displayConfig.getConfigurationSection("blackmarket-sold") :
                displayConfig.getConfigurationSection("sold");
        }
        
        if (typeConfig == null) {
            return new ItemStack(Material.PAPER);
        }
        
        Material material = Material.valueOf(typeConfig.getString("material", "PAPER"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set name
            String name = typeConfig.getString("name", "Transaction");
            name = name.replace("{item-name}", transaction.getItemName());
            name = name.replace('&', '§');
            meta.setDisplayName(name);
            
            // Set lore
            List<String> lore = typeConfig.getStringList("lore");
            List<String> newLore = new ArrayList<>();
            
            for (String line : lore) {
                line = line.replace("{price}", plugin.getEconomyManager().formatMoney(transaction.getPrice()));
                line = line.replace("{seller}", transaction.getSellerName());
                line = line.replace("{buyer}", transaction.getBuyerName());
                line = line.replace("{date}", transaction.getFormattedTimestamp());
                line = line.replace("{time-ago}", transaction.getTimeSince());
                line = line.replace('&', '§');
                newLore.add(line);
            }
            
            meta.setLore(newLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Create a GUI item from configuration
     */
    private ItemStack createGuiItem(ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", "STONE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set name
            String name = config.getString("name", "");
            if (!name.isEmpty()) {
                meta.setDisplayName(name.replace('&', '§'));
            }
            
            // Set lore
            List<String> lore = config.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace('&', '§'));
                }
                meta.setLore(coloredLore);
            }
            
            // Set custom model data
            int customModelData = config.getInt("custom-model-data", 0);
            if (customModelData != 0) {
                meta.setCustomModelData(customModelData);
            }
            
            // Set glow
            if (config.getBoolean("glow", false)) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Check if slot is a navigation slot
     */
    private boolean isNavigationSlot(int slot) {
        return slot >= 45; // Bottom row reserved for navigation
    }

    /**
     * Check if there's a next page
     */
    private boolean hasNextPage() {
        return (currentPage + 1) * itemsPerPage < transactions.size();
    }

    /**
     * Check if there's a previous page
     */
    private boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Get total number of pages
     */
    private int getTotalPages() {
        return (int) Math.ceil((double) transactions.size() / itemsPerPage);
    }

    /**
     * Go to next page
     */
    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
            refresh();
        }
    }

    /**
     * Go to previous page
     */
    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            refresh();
        }
    }

    /**
     * Refresh the GUI
     */
    public void refresh() {
        transactions.clear();
        transactions.addAll(plugin.getMongoManager().getTransactionsByPlayer(player.getUniqueId()));
        
        // Adjust current page if necessary
        int totalPages = getTotalPages();
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
        }
        
        populateInventory();
    }

    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Get the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
}
