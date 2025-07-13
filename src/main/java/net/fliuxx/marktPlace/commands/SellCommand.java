package net.fliuxx.marktPlace.commands;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.database.models.PlayerData;
import net.fliuxx.marktPlace.utils.ItemSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sell Command
 * Handles /sell command for listing items
 */
public class SellCommand implements CommandExecutor, TabCompleter {

    private final MarktPlace plugin;

    public SellCommand(MarktPlace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("marketplace.sell")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Check if price is provided
        if (args.length == 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§cUsage: /sell <price>");
            return true;
        }

        // Parse price
        double price;
        try {
            price = plugin.getEconomyManager().parseMoney(args[0]);
            if (price <= 0) {
                player.sendMessage(plugin.getConfigManager().getMessage("marketplace.invalid-price"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.invalid-price"));
            return true;
        }

        // Validate price
        if (!plugin.getEconomyManager().isValidAmount(price)) {
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.invalid-price"));
            return true;
        }

        // Check if player is holding an item
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!ItemSerializer.isValidItem(itemInHand)) {
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.no-item"));
            return true;
        }

        // Check max listings per player
        long activeListings = plugin.getMongoManager().getActiveListingsCount(player.getUniqueId());
        int maxListings = plugin.getConfig().getInt("general.max-listings-per-player", 10);
        
        if (activeListings >= maxListings) {
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.max-listings", 
                "max", String.valueOf(maxListings)));
            return true;
        }

        try {
            // Serialize item
            String itemData = ItemSerializer.serializeItemStack(itemInHand);
            
            // Create market item
            String itemId = UUID.randomUUID().toString();
            MarketItem marketItem = new MarketItem(
                itemId,
                player.getUniqueId(),
                player.getName(),
                itemInHand,
                itemData,
                price
            );

            // Check if identical item already exists
            List<MarketItem> existingItems = plugin.getMongoManager().getMarketItemsBySeller(player.getUniqueId());
            for (MarketItem existing : existingItems) {
                try {
                    ItemStack existingItemStack = ItemSerializer.deserializeItemStack(existing.getItemData());
                    if (ItemSerializer.areItemsEqual(itemInHand, existingItemStack) && 
                        existing.getPrice() == price) {
                        player.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-exists"));
                        return true;
                    }
                } catch (Exception e) {
                    // Skip corrupted items
                    continue;
                }
            }

            // Add to database
            plugin.getMongoManager().addMarketItem(marketItem);

            // Remove item from player's inventory
            player.getInventory().setItemInMainHand(null);

            // Auto-refresh all marketplace GUIs
            plugin.getGUIManager().refreshMarketplaceGUIs();
            plugin.getGUIManager().refreshMyItemsGUIs();

            // Update player data
            PlayerData playerData = plugin.getMongoManager().getPlayerData(player.getUniqueId());
            playerData.setPlayerName(player.getName());
            playerData.updateLastActive();
            plugin.getMongoManager().savePlayerData(playerData);

            // Send success message
            String itemName = ItemSerializer.getDisplayName(itemInHand);
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-listed",
                "item", itemName,
                "price", plugin.getEconomyManager().formatMoney(price)));

            // Log debug info
            if (plugin.getConfig().getBoolean("debug.log-transactions", true)) {
                plugin.getLogger().info(player.getName() + " listed " + itemName + " for " + 
                    plugin.getEconomyManager().formatMoney(price));
            }

        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.item-serialize-error"));
            plugin.getLogger().severe("Error listing item for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest common price values
            completions.add("10");
            completions.add("50");
            completions.add("100");
            completions.add("500");
            completions.add("1000");
        }

        return completions;
    }
}
