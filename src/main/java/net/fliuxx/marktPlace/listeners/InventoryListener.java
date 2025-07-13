package net.fliuxx.marktPlace.listeners;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.database.models.PlayerData;
import net.fliuxx.marktPlace.database.models.Transaction;
import net.fliuxx.marktPlace.gui.BlackMarketGUI;
import net.fliuxx.marktPlace.gui.ConfirmationGUI;
import net.fliuxx.marktPlace.gui.MarketplaceGUI;
import net.fliuxx.marktPlace.gui.MyItemsGUI;
import net.fliuxx.marktPlace.gui.TransactionHistoryGUI;
import net.fliuxx.marktPlace.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Inventory Listener
 * Handles all GUI interactions
 */
public class InventoryListener implements Listener {

    private final MarktPlace plugin;

    public InventoryListener(MarktPlace plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        // Check if it's a plugin GUI
        Object gui = plugin.getGUIManager().getGUI(player.getUniqueId());
        if (gui == null) {
            // Try to determine GUI type by inventory title
            String title = event.getView().getTitle();
            if (title.contains("MarketPlace")) {
                gui = new MarketplaceGUI(plugin, player);
                plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            } else if (title.contains("Black Market")) {
                gui = new BlackMarketGUI(plugin, player);
                plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            } else if (title.contains("My Items")) {
                gui = new MyItemsGUI(plugin, player);
                plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            } else if (title.contains("Transaction History")) {
                gui = new TransactionHistoryGUI(plugin, player);
                plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            } else if (title.contains("Confirm Purchase") || title.contains("§eConfirm Purchase")) {
                handleConfirmationClick(event, player, clickedItem);
                return;
            } else {
                return;
            }
        }

        event.setCancelled(true);

        // Handle different GUI types
        if (gui instanceof MarketplaceGUI) {
            handleMarketplaceClick(event, player, (MarketplaceGUI) gui, clickedItem);
        } else if (gui instanceof BlackMarketGUI) {
            handleBlackMarketClick(event, player, (BlackMarketGUI) gui, clickedItem);
        } else if (gui instanceof MyItemsGUI) {
            handleMyItemsClick(event, player, (MyItemsGUI) gui, clickedItem);
        } else if (gui instanceof TransactionHistoryGUI) {
            handleTransactionHistoryClick(event, player, (TransactionHistoryGUI) gui, clickedItem);
        }
    }

    /**
     * Handle marketplace GUI clicks
     */
    private void handleMarketplaceClick(InventoryClickEvent event, Player player, MarketplaceGUI gui, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();
        int slot = event.getSlot();

        // Handle navigation buttons
        if (displayName.contains("Next Page")) {
            gui.nextPage();
        } else if (displayName.contains("Previous Page")) {
            gui.previousPage();
        } else if (displayName.contains("Close")) {
            player.closeInventory();
        } else if (displayName.contains("My Items")) {
            MyItemsGUI myItemsGUI = new MyItemsGUI(plugin, player);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), myItemsGUI);
            myItemsGUI.open();
        } else {
            // Handle item clicks
            MarketItem item = gui.getMarketItemAtSlot(slot);
            if (item != null) {
                handleItemPurchase(player, item, false);
            }
        }
    }

    /**
     * Handle black market GUI clicks
     */
    private void handleBlackMarketClick(InventoryClickEvent event, Player player, BlackMarketGUI gui, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();
        int slot = event.getSlot();

        // Handle navigation buttons
        if (displayName.contains("Next Page")) {
            gui.nextPage();
        } else if (displayName.contains("Previous Page")) {
            gui.previousPage();
        } else if (displayName.contains("Close")) {
            player.closeInventory();
        } else if (displayName.contains("My Items")) {
            MyItemsGUI myItemsGUI = new MyItemsGUI(plugin, player);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), myItemsGUI);
            myItemsGUI.open();
        } else {
            // Handle item clicks
            MarketItem item = gui.getMarketItemAtSlot(slot);
            if (item != null) {
                handleItemPurchase(player, item, true);
            }
        }
    }

    /**
     * Handle My Items GUI clicks
     */
    private void handleMyItemsClick(InventoryClickEvent event, Player player, MyItemsGUI gui, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();
        int slot = event.getSlot();

        // Handle navigation buttons
        if (displayName.contains("Next Page")) {
            gui.nextPage();
        } else if (displayName.contains("Previous Page")) {
            gui.previousPage();
        } else if (displayName.contains("Close")) {
            player.closeInventory();
        } else if (displayName.contains("Back")) {
            // Return to marketplace
            MarketplaceGUI marketplaceGUI = new MarketplaceGUI(plugin, player);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), marketplaceGUI);
            marketplaceGUI.open();
        } else {
            // Handle item removal
            MarketItem item = gui.getMarketItemAtSlot(slot);
            if (item != null) {
                gui.removeItem(item);
                player.sendMessage(plugin.getConfigManager().getMessage("my-items.item-removed"));
            }
        }
    }

    /**
     * Handle transaction history GUI clicks
     */
    private void handleTransactionHistoryClick(InventoryClickEvent event, Player player, TransactionHistoryGUI gui, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // Handle navigation buttons
        if (displayName.contains("Next Page")) {
            gui.nextPage();
        } else if (displayName.contains("Previous Page")) {
            gui.previousPage();
        } else if (displayName.contains("Close")) {
            player.closeInventory();
        }
    }

    /**
     * Handle item purchase
     */
    private void handleItemPurchase(Player player, MarketItem item, boolean isBlackMarket) {
        // Check if player can buy their own items
        if (item.getSellerId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.cannot-buy-own"));
            return;
        }

        // Check if confirmation GUI is enabled
        if (plugin.getConfig().getBoolean("general.confirmation-gui", true)) {
            ConfirmationGUI confirmationGUI = new ConfirmationGUI(plugin, player, item, isBlackMarket);
            confirmationGUI.open();
            plugin.getGUIManager().registerGUI(player.getUniqueId(), confirmationGUI);
        } else {
            // Direct purchase
            processPurchase(player, item, isBlackMarket);
        }
    }

    /**
     * Handle confirmation GUI clicks
     */
    private void handleConfirmationClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);

        int slot = event.getSlot();

        // Find the confirmation GUI
        Object gui = plugin.getGUIManager().getGUI(player.getUniqueId());
        if (!(gui instanceof ConfirmationGUI)) {
            return;
        }

        ConfirmationGUI confirmationGUI = (ConfirmationGUI) gui;

        // Check by slot instead of display name to avoid issues with color codes and translations
        if (slot == 11) { // Confirm button slot
            MarketItem item = confirmationGUI.getMarketItem();
            boolean isBlackMarket = confirmationGUI.isBlackMarket();

            player.closeInventory();
            processPurchase(player, item, isBlackMarket);

        } else if (slot == 15) { // Cancel button slot
            player.closeInventory();
        }
    }

    /**
     * Process the actual purchase
     */
    private void processPurchase(Player player, MarketItem item, boolean isBlackMarket) {
        try {
            // Check if item still exists
            MarketItem currentItem = isBlackMarket ? 
                plugin.getMongoManager().getBlackMarketItem(item.getId()) :
                plugin.getMongoManager().getMarketItem(item.getId());

            if (currentItem == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-not-found"));
                return;
            }

            // Check if player has enough money
            if (!plugin.getEconomyManager().hasEnough(player, currentItem.getPrice())) {
                player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds",
                    "price", plugin.getEconomyManager().formatMoney(currentItem.getPrice())));
                return;
            }

            // Check if player has inventory space
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§cYour inventory is full!");
                return;
            }

            // Withdraw money from buyer
            if (!plugin.getEconomyManager().withdraw(player, currentItem.getPrice())) {
                player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
                return;
            }

            // Calculate seller payment
            double sellerPayment = currentItem.getPrice();
            if (isBlackMarket) {
                sellerPayment = plugin.getBlackMarketManager().calculateSellerProfit(currentItem.getOriginalPrice());
            }

            // Pay seller
            Player seller = Bukkit.getPlayer(currentItem.getSellerId());
            if (seller != null && seller.isOnline()) {
                plugin.getEconomyManager().deposit(seller, sellerPayment);
                seller.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-sold",
                    "item", currentItem.getItemData(),
                    "buyer", player.getName(),
                    "price", plugin.getEconomyManager().formatMoney(sellerPayment)));
            } else {
                // Seller is offline - could implement offline payment system
                // For now, just deposit to their account when they come online
                // TODO: Implement offline payment system
                // For now, log the payment for later processing
                plugin.getLogger().info("Offline payment pending for " + currentItem.getSellerName() + ": " + sellerPayment);
            }

            // Give item to buyer
            ItemStack itemStack = ItemSerializer.deserializeItemStack(currentItem.getItemData());
            player.getInventory().addItem(itemStack);

            // Remove item from marketplace
            if (isBlackMarket) {
                plugin.getMongoManager().removeBlackMarketItem(currentItem.getId());
                // Auto-refresh black market GUIs
                plugin.getGUIManager().refreshBlackMarketGUIs();
            } else {
                plugin.getMongoManager().removeMarketItem(currentItem.getId());
                // Auto-refresh marketplace GUIs
                plugin.getGUIManager().refreshMarketplaceGUIs();
            }

            // Auto-refresh My Items GUIs
            plugin.getGUIManager().refreshMyItemsGUIs();

            // Create transaction record
            Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                player.getUniqueId(),
                player.getName(),
                currentItem.getSellerId(),
                currentItem.getSellerName(),
                ItemSerializer.getDisplayName(itemStack),
                currentItem.getItemData(),
                currentItem.getPrice(),
                isBlackMarket ? Transaction.TransactionType.BLACK_MARKET : Transaction.TransactionType.NORMAL
            );

            plugin.getMongoManager().addTransaction(transaction);

            // Update player data
            PlayerData buyerData = plugin.getMongoManager().getPlayerData(player.getUniqueId());
            buyerData.setPlayerName(player.getName());
            buyerData.incrementItemsBought();
            buyerData.addSpent((long) (currentItem.getPrice() * 100)); // Convert to cents
            buyerData.updateLastActive();
            plugin.getMongoManager().savePlayerData(buyerData);

            PlayerData sellerData = plugin.getMongoManager().getPlayerData(currentItem.getSellerId());
            sellerData.setPlayerName(currentItem.getSellerName());
            sellerData.incrementItemsSold();
            sellerData.addEarnings((long) (sellerPayment * 100)); // Convert to cents
            plugin.getMongoManager().savePlayerData(sellerData);

            // Send success messages
            player.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-purchased",
                "item", ItemSerializer.getDisplayName(itemStack),
                "seller", currentItem.getSellerName(),
                "price", plugin.getEconomyManager().formatMoney(currentItem.getPrice())));

            // Send Discord webhook
            if (isBlackMarket) {
                plugin.getDiscordWebhook().sendBlackMarketPurchaseNotification(
                    player.getName(),
                    currentItem.getSellerName(),
                    ItemSerializer.getDisplayName(itemStack),
                    currentItem.getPrice()
                );
            } else {
                plugin.getDiscordWebhook().sendPurchaseNotification(
                    player.getName(),
                    currentItem.getSellerName(),
                    ItemSerializer.getDisplayName(itemStack),
                    currentItem.getPrice()
                );
            }

            // Log transaction
            if (plugin.getConfig().getBoolean("debug.log-transactions", true)) {
                plugin.getLogger().info(player.getName() + " purchased " + ItemSerializer.getDisplayName(itemStack) + 
                    " from " + currentItem.getSellerName() + " for " + plugin.getEconomyManager().formatMoney(currentItem.getPrice()) +
                    (isBlackMarket ? " (Black Market)" : ""));
            }

        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error processing purchase for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            plugin.getGUIManager().unregisterGUI(player.getUniqueId());
        }
    }
}