package net.fliuxx.marktPlace.gui;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.utils.ItemSerializer;
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
 * My Items GUI
 * Shows player's items currently for sale in both regular market and black market
 */
public class MyItemsGUI {

    private final MarktPlace plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<MarketItem> myItems;
    private final List<MarketItem> myBlackMarketItems;
    private int currentPage = 0;
    private final int itemsPerPage = 36; // 4 rows for items

    public MyItemsGUI(MarktPlace plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Load items from database
        this.myItems = plugin.getMongoManager().getPlayerMarketItems(player.getUniqueId());
        this.myBlackMarketItems = plugin.getMongoManager().getPlayerBlackMarketItems(player.getUniqueId());

        // Create inventory
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("my-items");
        String title = guiConfig.getString("title", "&6My Items").replace('&', '§');
        this.inventory = plugin.getServer().createInventory(null, 54, title);

        refresh();
    }

    /**
     * Refresh the GUI
     */
    public void refresh() {
        inventory.clear();

        // Reload items from database
        myItems.clear();
        myItems.addAll(plugin.getMongoManager().getPlayerMarketItems(player.getUniqueId()));
        myBlackMarketItems.clear();
        myBlackMarketItems.addAll(plugin.getMongoManager().getPlayerBlackMarketItems(player.getUniqueId()));

        // Ensure black market flag is set correctly for black market items
        for (MarketItem item : myBlackMarketItems) {
            item.setBlackMarket(true);
        }

        // Combine all items
        List<MarketItem> allItems = new ArrayList<>();
        allItems.addAll(myItems);
        allItems.addAll(myBlackMarketItems);

        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("my-items");

        // Add items to inventory
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            MarketItem item = allItems.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = createMyItemDisplay(item, guiConfig);
            
            // Add NBT identifier for market items
            displayItem = addMarketItemIdentifier(displayItem, item.getId());
            
            inventory.setItem(slot, displayItem);
        }

        // Add navigation and control buttons
        addNavigationButtons(guiConfig);
        addFillerItems(guiConfig);
    }

    /**
     * Create display item for player's own items
     */
    private ItemStack createMyItemDisplay(MarketItem item, ConfigurationSection guiConfig) {
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
                    line = line.replace("{market-type}", item.isBlackMarket() ? "Black Market" : "Market");
                    line = line.replace("{time-ago}", item.getTimeSinceListing());
                    line = line.replace('&', '§');
                    newLore.add(line);
                }

                // Note: removal instructions are already in guis.yml configuration

                meta.setLore(newLore);

                // Set glow for black market items
                if (item.isBlackMarket()) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            }

            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    /**
     * Add navigation buttons
     */
    private void addNavigationButtons(ConfigurationSection guiConfig) {
        // Previous page button
        if (hasPreviousPage()) {
            ConfigurationSection prevConfig = guiConfig.getConfigurationSection("buttons.previous-page");
            if (prevConfig != null) {
                int slot = prevConfig.getInt("slot", 45);
                ItemStack prevButton = createGuiItem(prevConfig);
                prevButton = addGuiButtonIdentifier(prevButton, "previous-page", slot);
                inventory.setItem(slot, prevButton);
            }
        }

        // Next page button
        if (hasNextPage()) {
            ConfigurationSection nextConfig = guiConfig.getConfigurationSection("buttons.next-page");
            if (nextConfig != null) {
                int slot = nextConfig.getInt("slot", 53);
                ItemStack nextButton = createGuiItem(nextConfig);
                nextButton = addGuiButtonIdentifier(nextButton, "next-page", slot);
                inventory.setItem(slot, nextButton);
            }
        }

        // Back to marketplace button
        ConfigurationSection backConfig = guiConfig.getConfigurationSection("buttons.back");
        if (backConfig != null) {
            int slot = backConfig.getInt("slot", 49);
            ItemStack backButton = createGuiItem(backConfig);
            backButton = addGuiButtonIdentifier(backButton, "back", slot);
            inventory.setItem(slot, backButton);
        }

        // Close button
        ConfigurationSection closeConfig = guiConfig.getConfigurationSection("buttons.close");
        if (closeConfig != null) {
            int slot = closeConfig.getInt("slot", 48);
            ItemStack closeButton = createGuiItem(closeConfig);
            closeButton = addGuiButtonIdentifier(closeButton, "close", slot);
            inventory.setItem(slot, closeButton);
        }

        // Page info
        ConfigurationSection pageInfoConfig = guiConfig.getConfigurationSection("page-info");
        if (pageInfoConfig != null) {
            Material material = Material.valueOf(pageInfoConfig.getString("material", "PAPER"));
            ItemStack pageInfo = new ItemStack(material);
            ItemMeta meta = pageInfo.getItemMeta();

            if (meta != null) {
                String name = pageInfoConfig.getString("name", "&6Page {current}/{total}");
                name = name.replace("{current}", String.valueOf(currentPage + 1));
                name = name.replace("{total}", String.valueOf(getTotalPages()));
                name = name.replace('&', '§');
                meta.setDisplayName(name);

                List<String> lore = pageInfoConfig.getStringList("lore");
                if (!lore.isEmpty()) {
                    List<String> newLore = new ArrayList<>();
                    int totalItems = myItems.size() + myBlackMarketItems.size();

                    for (String line : lore) {
                        line = line.replace("{market-items}", String.valueOf(myItems.size()));
                        line = line.replace("{blackmarket-items}", String.valueOf(myBlackMarketItems.size()));
                        line = line.replace("{total-items}", String.valueOf(totalItems));
                        line = line.replace('&', '§');
                        newLore.add(line);
                    }
                    meta.setLore(newLore);
                }

                pageInfo.setItemMeta(meta);
            }

            int slot = pageInfoConfig.getInt("slot", 50);
            pageInfo = addGuiButtonIdentifier(pageInfo, "page-info", slot);
            inventory.setItem(slot, pageInfo);
        }
    }

    /**
     * Add filler items
     */
    private void addFillerItems(ConfigurationSection guiConfig) {
        ConfigurationSection fillerConfig = guiConfig.getConfigurationSection("items.filler");
        if (fillerConfig != null && fillerConfig.getBoolean("enabled", true)) {
            List<Integer> slots = fillerConfig.getIntegerList("slots");
            ItemStack filler = createGuiItem(fillerConfig);

            for (int slot : slots) {
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, filler);
                }
            }
        }
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
     * Remove item from marketplace
     */
    public void removeItem(MarketItem item) {
        try {
            // Remove from database first
            boolean removed = false;
            if (item.isBlackMarket()) {
                plugin.getMongoManager().removeBlackMarketItem(item.getId());
                removed = true;
            } else {
                plugin.getMongoManager().removeMarketItem(item.getId());
                removed = true;
            }

            if (removed) {
                // Remove from local lists immediately to prevent duplication
                myItems.removeIf(marketItem -> marketItem.getId().equals(item.getId()));
                myBlackMarketItems.removeIf(marketItem -> marketItem.getId().equals(item.getId()));

                // Give item back to player
                ItemStack itemStack = ItemSerializer.deserializeItemStack(item.getItemData());

                // Check if player has space
                if (player.getInventory().firstEmpty() == -1) {
                    // Drop item on ground if inventory is full
                    player.getWorld().dropItem(player.getLocation(), itemStack);
                    player.sendMessage(plugin.getConfigManager().getMessage("my-items.item-dropped"));
                } else {
                    player.getInventory().addItem(itemStack);
                    player.sendMessage(plugin.getConfigManager().getMessage("my-items.item-removed"));
                }

                // Refresh GUI after local list update
                refresh();

                // Re-register this GUI to ensure it doesn't get lost
                plugin.getGUIManager().registerGUI(player.getUniqueId(), this);

                // Auto-refresh all marketplace GUIs
                plugin.getGUIManager().refreshMarketplaceGUIs();
                plugin.getGUIManager().refreshBlackMarketGUIs();
                plugin.getGUIManager().refreshMyItemsGUIs();
            }

        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().warning("Error removing item from marketplace: " + e.getMessage());
        }
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
        List<MarketItem> allItems = new ArrayList<>();
        allItems.addAll(myItems);
        allItems.addAll(myBlackMarketItems);
        return (currentPage + 1) * itemsPerPage < allItems.size();
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
        List<MarketItem> allItems = new ArrayList<>();
        allItems.addAll(myItems);
        allItems.addAll(myBlackMarketItems);
        return Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
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
     * Open the GUI for the player
     */
    public void open() {
        try {
            // Ensure GUI is properly registered before opening
            plugin.getGUIManager().registerGUI(player.getUniqueId(), this);

            // Add debug logging
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("Opening My Items GUI for player " + player.getName() + " - Items: " + (myItems.size() + myBlackMarketItems.size()));
            }

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.gui-error"));
            plugin.getLogger().severe("Error opening My Items GUI for " + player.getName() + ": " + e.getMessage());
        }
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
        
        // Find the market item by ID from both lists
        List<MarketItem> allItems = new ArrayList<>();
        allItems.addAll(myItems);
        allItems.addAll(myBlackMarketItems);
        
        return allItems.stream()
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