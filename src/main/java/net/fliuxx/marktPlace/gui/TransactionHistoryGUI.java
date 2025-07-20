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
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

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
        title = title.replace("{total}", String.valueOf(getTotalPages()));
        title = title.replace('&', '§');

        int rows = guiConfig.getInt("rows", 6);

        // Always create a new inventory with the updated title
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
                int slot = nextPageConfig.getInt("slot", 53);
                ItemStack nextPage = createGuiItem(nextPageConfig);
                nextPage = addGuiButtonIdentifier(nextPage, "next-page", slot);
                inventory.setItem(slot, nextPage);
            }
        }
        
        // Previous page button
        if (hasPreviousPage()) {
            ConfigurationSection prevPageConfig = guiConfig.getConfigurationSection("items.previous-page");
            if (prevPageConfig != null) {
                int slot = prevPageConfig.getInt("slot", 45);
                ItemStack prevPage = createGuiItem(prevPageConfig);
                prevPage = addGuiButtonIdentifier(prevPage, "previous-page", slot);
                inventory.setItem(slot, prevPage);
            }
        }
        
        // Close button
        ConfigurationSection closeConfig = guiConfig.getConfigurationSection("items.close");
        if (closeConfig != null) {
            int slot = closeConfig.getInt("slot", 49);
            ItemStack close = createGuiItem(closeConfig);
            close = addGuiButtonIdentifier(close, "close", slot);
            inventory.setItem(slot, close);
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

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("TransactionHistory addTransactionItems() - Player: " + player.getName() +
                    ", Page: " + currentPage + ", StartIndex: " + startIndex +
                    ", EndIndex: " + endIndex + ", Total transactions: " + transactions.size());
        }

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

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("TransactionHistory - Added " + (endIndex - startIndex) + " items to page " + currentPage);
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
        boolean result = (currentPage + 1) * itemsPerPage < transactions.size();

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("hasNextPage() - Player: " + player.getName() +
                    ", currentPage: " + currentPage + ", itemsPerPage: " + itemsPerPage +
                    ", transactions.size(): " + transactions.size() + ", result: " + result);
        }

        return result;
    }

    /**
     * Check if there's a previous page
     */
    private boolean hasPreviousPage() {
        boolean result = currentPage > 0;

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("hasPreviousPage() - Player: " + player.getName() +
                    ", currentPage: " + currentPage + ", result: " + result);
        }

        return result;
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

            // Debug logging
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("TransactionHistory nextPage() - Player: " + player.getName() +
                        ", New page: " + currentPage + ", Total transactions: " + transactions.size());
            }

            refresh();
        } else {
            // Debug logging
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("TransactionHistory nextPage() - No next page available - Player: " + player.getName() +
                        ", Current page: " + currentPage + ", Total transactions: " + transactions.size());
            }
        }
    }

    /**
     * Go to previous page
     */
    public void previousPage() {
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("previousPage() called - Player: " + player.getName() +
                    ", Current page: " + currentPage + ", Has previous: " + hasPreviousPage());
        }

        if (hasPreviousPage()) {
            int oldPage = currentPage;
            currentPage--;

            // Ensure currentPage doesn't go negative
            if (currentPage < 0) {
                currentPage = 0;
            }

            // Debug logging
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("TransactionHistory previousPage() - Player: " + player.getName() +
                        ", Old page: " + oldPage + ", New page: " + currentPage + ", Total transactions: " + transactions.size());
            }

            refresh();
        } else {
            // Debug logging
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("TransactionHistory previousPage() - No previous page available - Player: " + player.getName() +
                        ", Current page: " + currentPage + ", Total transactions: " + transactions.size());
            }
        }
    }

    /**
     * Refresh the GUI
     */
    public void refresh() {
        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("TransactionHistory refresh() - Player: " + player.getName() +
                    ", Current page: " + currentPage + ", Old transactions size: " + transactions.size());
        }

        // Get fresh transaction data
        List<Transaction> freshTransactions = plugin.getMongoManager().getTransactionsByPlayer(player.getUniqueId());

        // Update transactions list
        transactions.clear();
        transactions.addAll(freshTransactions);

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("TransactionHistory refresh() - Player: " + player.getName() +
                    ", New transactions size: " + transactions.size());
        }

        // Adjust current page if necessary (only if we have transactions)
        if (!transactions.isEmpty()) {
            int totalPages = getTotalPages();
            if (currentPage >= totalPages) {
                currentPage = Math.max(0, totalPages - 1);
            }
        } else {
            currentPage = 0;
        }

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("TransactionHistory refresh() - Player: " + player.getName() +
                    ", Final page: " + currentPage + ", Total pages: " + getTotalPages());
        }

        // Recreate inventory with updated title
        createInventory();

        // Re-open the inventory for the player to see the changes
        if (player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && plugin.getGUIManager().getGUI(player.getUniqueId()) instanceof TransactionHistoryGUI) {
                    player.openInventory(inventory);

                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("TransactionHistory - Reopened inventory for player: " + player.getName());
                    }
                }
            });
        }
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

    /**
     * Check if item at slot is a GUI button
     */
    public String getButtonType(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("getButtonType() - No item or meta at slot " + slot);
            }
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("getButtonType() - No meta at slot " + slot);
            }
            return null;
        }

        NamespacedKey key = new NamespacedKey(plugin, "gui_button_type");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            // Fallback: check by slot position for common navigation buttons
            ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("transactions");
            if (guiConfig != null) {
                if (slot == guiConfig.getInt("items.previous-page.slot", 45)) {
                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("getButtonType() - Fallback detected previous-page at slot " + slot);
                    }
                    return "previous-page";
                }
                if (slot == guiConfig.getInt("items.next-page.slot", 53)) {
                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("getButtonType() - Fallback detected next-page at slot " + slot);
                    }
                    return "next-page";
                }
                if (slot == guiConfig.getInt("items.close.slot", 49)) {
                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("getButtonType() - Fallback detected close at slot " + slot);
                    }
                    return "close";
                }
            }

            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("getButtonType() - No NBT data found at slot " + slot);
            }
            return null;
        }

        String buttonType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("getButtonType() - Found button type '" + buttonType + "' at slot " + slot);
        }

        return buttonType;
    }

    /**
     * Check if item at slot is a GUI button
     */
    public boolean isGuiButton(int slot) {
        return getButtonType(slot) != null;
    }

    /**
     * Add NBT identifier to GUI button
     */
    private ItemStack addGuiButtonIdentifier(ItemStack item, String buttonType, int slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "gui_button_type");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buttonType);
            item.setItemMeta(meta);
        }
        return item;
    }
}
