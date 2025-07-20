package net.fliuxx.marktPlace.gui;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
        
        // My Items button
        ConfigurationSection myItemsConfig = guiConfig.getConfigurationSection("items.my-items");
        if (myItemsConfig != null) {
            int slot = myItemsConfig.getInt("slot", 47);
            ItemStack myItems = createGuiItem(myItemsConfig);
            myItems = addGuiButtonIdentifier(myItems, "my-items", slot);
            inventory.setItem(slot, myItems);
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
        
        Set<Integer> reservedSlots = getReservedSlots(guiConfig);
        int rows = guiConfig.getInt("rows", 6);
        int maxSlot = rows * 9;
        
        int currentSlot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            // Find next available slot
            int availableSlot = getNextAvailableSlot(currentSlot, reservedSlots, maxSlot);
            if (availableSlot == -1) break; // No more available slots
            
            MarketItem item = items.get(i);
            ItemStack displayItem = createMarketItemDisplay(item, guiConfig);
            
            // Add NBT identifier for market items
            displayItem = addMarketItemIdentifier(displayItem, item.getId());
            
            inventory.setItem(availableSlot, displayItem);
            currentSlot = availableSlot + 1;
        }
    }
    
    /**
     * Add NBT identifier to market item
     */
    private ItemStack addMarketItemIdentifier(ItemStack item, String marketItemId) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "marketplace_item_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, marketItemId);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Add page information
     */
    private void addPageInfo(ConfigurationSection guiConfig) {
        ConfigurationSection pageInfoConfig = guiConfig.getConfigurationSection("items.page-info");
        if (pageInfoConfig != null) {
            int slot = pageInfoConfig.getInt("slot", 51);
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
            
            pageInfo = addGuiButtonIdentifier(pageInfo, "page-info", slot);
            inventory.setItem(slot, pageInfo);
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
     * Create a GUI item from configuration with NBT identifier
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
     * Add GUI button identifier to distinguish from market items
     */
    private ItemStack addGuiButtonIdentifier(ItemStack item, String buttonType, int slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_button_type");
            NamespacedKey slotKey = new NamespacedKey(plugin, "gui_button_slot");
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, buttonType);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get all reserved slots (navigation, filler, etc.)
     */
    private Set<Integer> getReservedSlots(ConfigurationSection guiConfig) {
        Set<Integer> reservedSlots = new HashSet<>();
        
        // Add navigation item slots
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig != null) {
            for (String key : itemsConfig.getKeys(false)) {
                ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(key);
                if (itemConfig != null) {
                    int slot = itemConfig.getInt("slot", -1);
                    if (slot >= 0) {
                        reservedSlots.add(slot);
                    }
                }
            }
        }
        
        // Add filler slots
        ConfigurationSection fillerConfig = guiConfig.getConfigurationSection("items.filler");
        if (fillerConfig != null && fillerConfig.getBoolean("enabled", true)) {
            List<Integer> fillerSlots = fillerConfig.getIntegerList("slots");
            reservedSlots.addAll(fillerSlots);
        }
        
        return reservedSlots;
    }
    
    /**
     * Check if slot is reserved for navigation or filler
     */
    private boolean isReservedSlot(int slot, Set<Integer> reservedSlots) {
        return reservedSlots.contains(slot);
    }
    
    /**
     * Get next available slot for market items
     */
    private int getNextAvailableSlot(int startSlot, Set<Integer> reservedSlots, int maxSlot) {
        for (int slot = startSlot; slot < maxSlot; slot++) {
            if (!isReservedSlot(slot, reservedSlots)) {
                return slot;
            }
        }
        return -1; // No available slot found
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
     * Get market item at slot by checking NBT data
     */
    public MarketItem getMarketItemAtSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "marketplace_item_id");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return null; // Not a market item
        }
        
        String marketItemId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (marketItemId == null) return null;
        
        // Find the market item by ID
        return items.stream()
            .filter(marketItem -> marketItem.getId().equals(marketItemId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if item at slot is a GUI button
     */
    public String getButtonType(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "gui_button_type");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return null;
        }
        
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
    
    /**
     * Check if item at slot is a market item
     */
    public boolean isMarketItem(int slot) {
        return getMarketItemAtSlot(slot) != null;
    }
    
    /**
     * Check if item at slot is a GUI button
     */
    public boolean isGuiButton(int slot) {
        return getButtonType(slot) != null;
    }
}
