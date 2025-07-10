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
 * Confirmation GUI
 * Handles purchase confirmation interface
 */
public class ConfirmationGUI {

    private final MarktPlace plugin;
    private final Player player;
    private final MarketItem item;
    private final boolean isBlackMarket;
    private Inventory inventory;

    public ConfirmationGUI(MarktPlace plugin, Player player, MarketItem item, boolean isBlackMarket) {
        this.plugin = plugin;
        this.player = player;
        this.item = item;
        this.isBlackMarket = isBlackMarket;
        
        createInventory();
    }

    /**
     * Create the inventory
     */
    private void createInventory() {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("confirmation");
        
        String title = guiConfig.getString("title", "&eConfirm Purchase");
        title = title.replace('&', '§');
        
        int rows = guiConfig.getInt("rows", 3);
        
        inventory = Bukkit.createInventory(null, rows * 9, title);
        
        populateInventory();
    }

    /**
     * Populate the inventory with items
     */
    private void populateInventory() {
        inventory.clear();
        
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("confirmation");
        
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
     * Add confirm button
     */
    private void addConfirmButton(ConfigurationSection guiConfig) {
        ConfigurationSection confirmConfig = guiConfig.getConfigurationSection("items.confirm");
        if (confirmConfig != null) {
            ItemStack confirm = createGuiItem(confirmConfig);
            inventory.setItem(confirmConfig.getInt("slot", 11), confirm);
        }
    }

    /**
     * Add cancel button
     */
    private void addCancelButton(ConfigurationSection guiConfig) {
        ConfigurationSection cancelConfig = guiConfig.getConfigurationSection("items.cancel");
        if (cancelConfig != null) {
            ItemStack cancel = createGuiItem(cancelConfig);
            inventory.setItem(cancelConfig.getInt("slot", 15), cancel);
        }
    }

    /**
     * Add item information
     */
    private void addItemInfo(ConfigurationSection guiConfig) {
        ConfigurationSection itemInfoConfig = guiConfig.getConfigurationSection("items.item-info");
        if (itemInfoConfig != null) {
            ItemStack itemInfo;
            
            try {
                itemInfo = ItemSerializer.deserializeItemStack(item.getItemData());
            } catch (Exception e) {
                itemInfo = new ItemStack(Material.BARRIER);
            }
            
            ItemMeta meta = itemInfo.getItemMeta();
            if (meta != null) {
                // Set name
                String name = itemInfoConfig.getString("name", "&eItem Information");
                name = name.replace('&', '§');
                meta.setDisplayName(name);
                
                // Set lore
                List<String> lore = itemInfoConfig.getStringList("lore");
                List<String> newLore = new ArrayList<>();
                
                for (String line : lore) {
                    line = line.replace("{item-name}", ItemSerializer.getDisplayName(itemInfo));
                    line = line.replace("{price}", plugin.getEconomyManager().formatMoney(item.getPrice()));
                    line = line.replace("{seller}", item.getSellerName());
                    
                    if (isBlackMarket) {
                        line = line.replace("{original-price}", plugin.getEconomyManager().formatMoney(item.getOriginalPrice()));
                    }
                    
                    line = line.replace('&', '§');
                    newLore.add(line);
                }
                
                // Add black market specific info
                if (isBlackMarket) {
                    newLore.add("");
                    newLore.add("§c§lBLACK MARKET ITEM");
                    newLore.add("§7Original Price: §6" + plugin.getEconomyManager().formatMoney(item.getOriginalPrice()));
                    newLore.add("§7Sale Price: §c" + plugin.getEconomyManager().formatMoney(item.getPrice()));
                    newLore.add("§7Discount: §a50%");
                }
                
                meta.setLore(newLore);
                
                // Set glow for black market items
                if (isBlackMarket) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                
                itemInfo.setItemMeta(meta);
            }
            
            inventory.setItem(itemInfoConfig.getInt("slot", 13), itemInfo);
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
                inventory.setItem(slot, filler);
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
     * Check if this is a black market item
     */
    public boolean isBlackMarket() {
        return isBlackMarket;
    }
}
