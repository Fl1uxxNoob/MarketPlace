package net.fliuxx.marktPlace.gui;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.utils.ItemSerializer;
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
 * Marketplace GUI
 * Handles the main marketplace interface
 */
public class MarketplaceGUI {

    private final MarktPlace plugin;
    private final Player player;
    private final List<MarketItem> items;
    private final int itemsPerPage;
    private int currentPage;
    private Inventory inventory;

    public MarketplaceGUI(MarktPlace plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.items = plugin.getMongoManager().getAllMarketItems();
        this.itemsPerPage = 45; // 9x5 grid for items
        this.currentPage = 0;
        
        createInventory();
    }

    /**
     * Create the inventory
     */
    private void createInventory() {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("marketplace");
        
        String title = guiConfig.getString("title", "&6MarketPlace - Page {page}");
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
        
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("marketplace");
        
        // Add navigation items
        addNavigationItems(guiConfig);
        
        // Add filler items
        addFillerItems(guiConfig);
        
        // Add marketplace items
        addMarketItems(guiConfig);
        
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
        
        // My Items button
        ConfigurationSection myItemsConfig = guiConfig.getConfigurationSection("items.my-items");
        if (myItemsConfig != null) {
            ItemStack myItems = createGuiItem(myItemsConfig);
            inventory.setItem(myItemsConfig.getInt("slot", 47), myItems);
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
     * Add marketplace items to inventory
     */
    private void addMarketItems(ConfigurationSection guiConfig) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            // Skip navigation slots
            while (isNavigationSlot(slot)) {
                slot++;
            }
            
            if (slot >= 45) break; // Don't go beyond item area
            
            MarketItem item = items.get(i);
            ItemStack displayItem = createMarketItemDisplay(item, guiConfig);
            
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
     * Create a market item display
     */
    private ItemStack createMarketItemDisplay(MarketItem item, ConfigurationSection guiConfig) {
        ItemStack itemStack;
        
        try {
            itemStack = ItemSerializer.deserializeItemStack(item.getItemData());
        } catch (Exception e) {
            itemStack = new ItemStack(Material.BARRIER);
        }
        
        ItemStack displayItem = itemStack.clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta != null) {
            ConfigurationSection displayConfig = guiConfig.getConfigurationSection("item-display");
            if (displayConfig != null) {
                // Set name
                String name = displayConfig.getString("name", "&e{item-name}");
                name = name.replace("{item-name}", ItemSerializer.getDisplayName(itemStack));
                name = name.replace('&', '§');
                meta.setDisplayName(name);
                
                // Set lore
                List<String> lore = displayConfig.getStringList("lore");
                List<String> newLore = new ArrayList<>();
                
                for (String line : lore) {
                    line = line.replace("{price}", plugin.getEconomyManager().formatMoney(item.getPrice()));
                    line = line.replace("{seller}", item.getSellerName());
                    line = line.replace("{time-ago}", item.getTimeSinceListing());
                    line = line.replace('&', '§');
                    newLore.add(line);
                }
                
                meta.setLore(newLore);
                
                // Set glow
                if (displayConfig.getBoolean("glow", false)) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            }
            
            displayItem.setItemMeta(meta);
        }
        
        return displayItem;
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
        return (currentPage + 1) * itemsPerPage < items.size();
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
        return (int) Math.ceil((double) items.size() / itemsPerPage);
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
        items.clear();
        items.addAll(plugin.getMongoManager().getAllMarketItems());
        
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

    /**
     * Get item at slot
     */
    public MarketItem getMarketItemAtSlot(int slot) {
        if (slot >= 45 || slot < 0) return null;
        
        int itemIndex = (currentPage * itemsPerPage) + slot;
        
        // Count navigation slots before this slot
        int navigationSlots = 0;
        for (int i = 0; i < slot; i++) {
            if (isNavigationSlot(i)) {
                navigationSlots++;
            }
        }
        
        itemIndex -= navigationSlots;
        
        if (itemIndex >= 0 && itemIndex < items.size()) {
            return items.get(itemIndex);
        }
        
        return null;
    }
}
