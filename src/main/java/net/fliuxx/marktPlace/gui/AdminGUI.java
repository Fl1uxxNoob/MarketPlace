package net.fliuxx.marktPlace.gui;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin GUI
 * Handles the admin marketplace management interface
 */
public class AdminGUI {

    private final MarktPlace plugin;
    private final Player player;
    private final List<MarketItem> items;
    private final int itemsPerPage;
    private int currentPage;
    private Inventory inventory;

    public AdminGUI(MarktPlace plugin, Player player) {
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
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("admin");
        
        String title = guiConfig.getString("title", "&cAdmin Panel - Page {page}");
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
        
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("admin");
        
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
     * Add marketplace items to inventory
     */
    private void addMarketItems(ConfigurationSection guiConfig) {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        
        for (int i = start; i < end; i++) {
            MarketItem item = items.get(i);
            ItemStack displayItem = createMarketItemDisplay(item, guiConfig);
            
            // Find next available slot (skipping navigation slots)
            int slot = findNextAvailableSlot(i - start);
            if (slot != -1) {
                inventory.setItem(slot, displayItem);
            }
        }
    }

    /**
     * Create a market item display
     */
    private ItemStack createMarketItemDisplay(MarketItem marketItem, ConfigurationSection guiConfig) {
        ItemStack originalItem = ItemSerializer.deserializeItemStack(marketItem.getItemData());
        if (originalItem == null) {
            originalItem = new ItemStack(Material.BARRIER);
        }
        
        ItemStack displayItem = originalItem.clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta != null) {
            // Get display configuration
            ConfigurationSection displayConfig = guiConfig.getConfigurationSection("item-display");
            
            // Set custom name
            String name = displayConfig.getString("name", "&e{item-name}");
            name = name.replace("{item-name}", originalItem.getType().name());
            name = name.replace('&', '§');
            meta.setDisplayName(name);
            
            // Set lore
            List<String> lore = new ArrayList<>();
            List<String> loreTemplate = displayConfig.getStringList("lore");
            
            for (String line : loreTemplate) {
                line = line.replace("{price}", plugin.getEconomyManager().formatMoney(marketItem.getPrice()));
                line = line.replace("{seller}", marketItem.getSellerName());
                line = line.replace("{item-id}", marketItem.getId());
                line = line.replace('&', '§');
                lore.add(line);
            }
            
            meta.setLore(lore);
            
            // Add item identifier
            NamespacedKey key = new NamespacedKey(plugin, "market-item-id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, marketItem.getId());
            
            displayItem.setItemMeta(meta);
        }
        
        return displayItem;
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
                        line = line.replace("{items}", String.valueOf(items.size()));
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
     * Find next available slot
     */
    private int findNextAvailableSlot(int index) {
        // Calculate slot position avoiding navigation area (bottom row)
        int row = index / 9;
        int col = index % 9;
        
        // If we're at row 5 (bottom row), skip it
        if (row >= 5) {
            return -1;
        }
        
        return row * 9 + col;
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
     * Add GUI button identifier
     */
    private ItemStack addGuiButtonIdentifier(ItemStack item, String buttonType, int slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "gui-button");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buttonType);
            
            NamespacedKey slotKey = new NamespacedKey(plugin, "gui-slot");
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Open the GUI
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Handle page navigation
     */
    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
            createInventory();
            player.openInventory(inventory);
        }
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            createInventory();
            player.openInventory(inventory);
        }
    }

    /**
     * Handle item confiscation (right click)
     */
    public void confiscateItem(String itemId) {
        try {
            MarketItem item = plugin.getMongoManager().getMarketItem(itemId);
            if (item != null) {
                // Remove from marketplace
                plugin.getMongoManager().removeMarketItem(itemId);
                
                // Log the action
                plugin.getLogger().info("Admin " + player.getName() + " confiscated item " + itemId + " from " + item.getSellerName());
                
                // Send message to admin - use itemData instead of getItemStack()
                ItemStack originalItem = ItemSerializer.deserializeItemStack(item.getItemData());
                String itemName = originalItem != null ? originalItem.getType().name() : "Unknown Item";
                String message = plugin.getConfigManager().getMessage("admin.item-confiscated", 
                    "item", itemName,
                    "seller", item.getSellerName());
                player.sendMessage(message);
                
                // Refresh the GUI
                items.removeIf(i -> i.getId().equals(itemId));
                createInventory();
                player.openInventory(inventory);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error confiscating item " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Handle item return (left click)
     */
    public void returnItem(String itemId) {
        try {
            MarketItem item = plugin.getMongoManager().getMarketItem(itemId);
            if (item != null) {
                // Get the seller player
                OfflinePlayer seller = Bukkit.getOfflinePlayer(item.getSellerId());
                
                // Remove from marketplace
                plugin.getMongoManager().removeMarketItem(itemId);
                
                // If seller is online, give item directly, otherwise log for manual handling
                if (seller.isOnline()) {
                    Player onlineSeller = seller.getPlayer();
                    ItemStack originalItem = ItemSerializer.deserializeItemStack(item.getItemData());
                    
                    if (originalItem != null && onlineSeller.getInventory().firstEmpty() != -1) {
                        onlineSeller.getInventory().addItem(originalItem);
                        onlineSeller.sendMessage(plugin.getConfigManager().getMessage("admin.item-returned-to-you"));
                    } else {
                        // Inventory full or item error - log for manual handling
                        plugin.getLogger().warning("Could not return item " + itemId + " to " + seller.getName() + " - inventory full or item error");
                        player.sendMessage("§cCould not return item to " + seller.getName() + " - check console for details");
                        return;
                    }
                } else {
                    // Player offline - log for manual handling
                    plugin.getLogger().info("Item " + itemId + " from offline player " + seller.getName() + " was returned by admin " + player.getName() + " - manual intervention may be required");
                }
                
                // Log the action
                plugin.getLogger().info("Admin " + player.getName() + " returned item " + itemId + " to " + item.getSellerName());
                
                // Send message to admin - use itemData instead of getItemStack()
                ItemStack originalItem = ItemSerializer.deserializeItemStack(item.getItemData());
                String itemName = originalItem != null ? originalItem.getType().name() : "Unknown Item";
                String message = plugin.getConfigManager().getMessage("admin.item-returned", 
                    "item", itemName,
                    "seller", item.getSellerName());
                player.sendMessage(message);
                
                // Refresh the GUI
                items.removeIf(i -> i.getId().equals(itemId));
                createInventory();
                player.openInventory(inventory);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error returning item " + itemId + ": " + e.getMessage());
        }
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
     * Get total pages
     */
    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
    }

    /**
     * Check if slot is clickable button
     */
    public String getButtonType(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "gui-button");
        
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        
        return null;
    }

    /**
     * Get market item ID from slot
     */
    public String getMarketItemId(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "market-item-id");
        
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        
        return null;
    }

    /**
     * Check if slot is a button
     */
    public boolean isButton(int slot) {
        return getButtonType(slot) != null;
    }
}