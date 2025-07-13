
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
 * Confirmation GUI for purchase confirmation
 */
public class ConfirmationGUI {
    
    private final MarktPlace plugin;
    private final Player player;
    private final MarketItem item;
    private final Inventory inventory;
    private final boolean isBlackMarket;
    
    public ConfirmationGUI(MarktPlace plugin, Player player, MarketItem item, boolean isBlackMarket) {
        this.plugin = plugin;
        this.player = player;
        this.item = item;
        this.isBlackMarket = isBlackMarket;
        
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("confirmation");
        String title = guiConfig != null ? guiConfig.getString("title", "&eConfirm Purchase") : "&eConfirm Purchase";
        title = title.replace('&', '§');
        
        int rows = guiConfig != null ? guiConfig.getInt("rows", 3) : 3;
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
        
        setupGUI();
    }
    
    /**
     * Setup the GUI with all items
     */
    private void setupGUI() {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("confirmation");
        if (guiConfig == null) {
            setupDefaultGUI();
            return;
        }
        
        // Add confirm button
        addConfirmButton(guiConfig);
        
        // Add cancel button
        addCancelButton(guiConfig);
        
        // Add item info
        addItemInfo(guiConfig);
        
        // Add filler items
        addFillerItems(guiConfig);
    }
    
    /**
     * Setup default GUI if config is missing
     */
    private void setupDefaultGUI() {
        // Confirm button (slot 11)
        ItemStack confirmButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm Purchase");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§7Click to confirm the purchase");
            confirmMeta.setLore(confirmLore);
            confirmButton.setItemMeta(confirmMeta);
        }
        inventory.setItem(11, confirmButton);
        
        // Cancel button (slot 15)
        ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cCancel");
            List<String> cancelLore = new ArrayList<>();
            cancelLore.add("§7Click to cancel the purchase");
            cancelMeta.setLore(cancelLore);
            cancelButton.setItemMeta(cancelMeta);
        }
        inventory.setItem(15, cancelButton);
        
        // Item info (slot 13)
        ItemStack itemInfo = item.getItemStack().clone();
        ItemMeta itemMeta = itemInfo.getItemMeta();
        if (itemMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Price: §6$" + item.getPrice());
            lore.add("§7Seller: §6" + item.getSellerName());
            lore.add("");
            lore.add("§7Are you sure you want to purchase this item?");
            itemMeta.setLore(lore);
            itemInfo.setItemMeta(itemMeta);
        }
        inventory.setItem(13, itemInfo);
        
        // Add gray glass panes as filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    /**
     * Add confirm button
     */
    private void addConfirmButton(ConfigurationSection guiConfig) {
        ConfigurationSection confirmConfig = guiConfig.getConfigurationSection("items.confirm");
        if (confirmConfig != null) {
            ItemStack confirmButton = createGuiItem(confirmConfig);
            int slot = confirmConfig.getInt("slot", 11);
            inventory.setItem(slot, confirmButton);
        }
    }
    
    /**
     * Add cancel button
     */
    private void addCancelButton(ConfigurationSection guiConfig) {
        ConfigurationSection cancelConfig = guiConfig.getConfigurationSection("items.cancel");
        if (cancelConfig != null) {
            ItemStack cancelButton = createGuiItem(cancelConfig);
            int slot = cancelConfig.getInt("slot", 15);
            inventory.setItem(slot, cancelButton);
        }
    }
    
    /**
     * Add item info
     */
    private void addItemInfo(ConfigurationSection guiConfig) {
        ConfigurationSection itemConfig = guiConfig.getConfigurationSection("items.item-info");
        if (itemConfig != null) {
            ItemStack itemInfo = item.getItemStack().clone();
            ItemMeta meta = itemInfo.getItemMeta();
            if (meta != null) {
                // Set display name
                String name = itemConfig.getString("name", "&eItem Information");
                name = name.replace('&', '§');
                name = replacePlaceholders(name);
                meta.setDisplayName(name);
                
                // Set lore
                List<String> configLore = itemConfig.getStringList("lore");
                List<String> lore = new ArrayList<>();
                for (String line : configLore) {
                    line = line.replace('&', '§');
                    line = replacePlaceholders(line);
                    lore.add(line);
                }
                meta.setLore(lore);
                
                itemInfo.setItemMeta(meta);
            }
            
            int slot = itemConfig.getInt("slot", 13);
            inventory.setItem(slot, itemInfo);
        }
    }
    
    /**
     * Replace placeholders in text
     */
    private String replacePlaceholders(String text) {
        String itemName = "Unknown Item";
        if (item.getItemStack() != null && item.getItemStack().hasItemMeta() && 
            item.getItemStack().getItemMeta().hasDisplayName()) {
            itemName = item.getItemStack().getItemMeta().getDisplayName();
        } else if (item.getItemStack() != null) {
            itemName = item.getItemStack().getType().toString().replace("_", " ").toLowerCase();
            itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
        }
        
        return text
            .replace("{item-name}", itemName)
            .replace("{price}", String.format("%.2f", item.getPrice()))
            .replace("{seller}", item.getSellerName());
    }
    
    /**
     * Add filler items
     */
    private void addFillerItems(ConfigurationSection guiConfig) {
        ConfigurationSection fillerConfig = guiConfig.getConfigurationSection("items.filler");
        if (fillerConfig != null && fillerConfig.getBoolean("enabled", false)) {
            ItemStack filler = createGuiItem(fillerConfig);
            List<Integer> slots = fillerConfig.getIntegerList("slots");
            
            for (int slot : slots) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, filler);
                }
            }
        }
    }
    
    /**
     * Create a GUI item from configuration
     */
    private ItemStack createGuiItem(ConfigurationSection config) {
        String materialName = config.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String name = config.getString("name", "");
            if (!name.isEmpty()) {
                name = name.replace('&', '§');
                meta.setDisplayName(name);
            }
            
            // Set lore
            List<String> configLore = config.getStringList("lore");
            if (!configLore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : configLore) {
                    lore.add(line.replace('&', '§'));
                }
                meta.setLore(lore);
            }
            
            // Set custom model data
            if (config.contains("custom-model-data")) {
                meta.setCustomModelData(config.getInt("custom-model-data"));
            }
            
            item.setItemMeta(meta);
        }

        return item;
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
     * Get the market item
     */
    public MarketItem getMarketItem() {
        return item;
    }

    /**
     * Check if this is a black market purchase
     */
    public boolean isBlackMarket() {
        return isBlackMarket;
    }
}
