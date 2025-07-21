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
import net.fliuxx.marktPlace.gui.AdminGUI;
import net.fliuxx.marktPlace.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        // Only handle player clicks
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is a marketplace GUI
        boolean isMarketplaceGUI = isMarketplaceInventory(title);
        
        // If it's not a marketplace GUI, don't interfere with normal inventory interaction
        if (!isMarketplaceGUI) {
            return;
        }
        
        // SMART ANTI-THEFT SYSTEM: Only block unauthorized item manipulation
        if (isUnauthorizedItemManipulation(event)) {
            event.setCancelled(true);
            handleUnauthorizedAction(player, event);
            return;
        }
        
        // Cancel the event for GUI interactions but allow navigation
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return; // No item to interact with
        }
        
        // Get registered GUI for this player
        Object gui = plugin.getGUIManager().getGUI(player.getUniqueId());
        
        // Handle confirmation GUI specially
        if (title.contains("Confirm Purchase") || title.contains("§eConfirm Purchase")) {
            handleConfirmationClick(event, player, clickedItem);
            return;
        }
        
        // If GUI is not registered, try to determine by title and register it
        if (gui == null) {
            gui = createGUIFromTitle(title, player);
            if (gui != null) {
                plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            } else {
                return; // Couldn't determine GUI type, don't handle
            }
        }

        // Handle different GUI types
        if (gui instanceof MarketplaceGUI) {
            handleMarketplaceClick(event, player, (MarketplaceGUI) gui, clickedItem);
        } else if (gui instanceof BlackMarketGUI) {
            handleBlackMarketClick(event, player, (BlackMarketGUI) gui, clickedItem);
        } else if (gui instanceof MyItemsGUI) {
            handleMyItemsClick(event, player, (MyItemsGUI) gui, clickedItem);
        } else if (gui instanceof TransactionHistoryGUI) {
            handleTransactionHistoryClick(event, player, (TransactionHistoryGUI) gui, clickedItem);
        } else if (gui instanceof AdminGUI) {
            handleAdminClick(event, player, (AdminGUI) gui, clickedItem);
        }
    }
    
    /**
     * Check if an inventory title belongs to a marketplace GUI
     * This is the critical function that prevents interference with normal inventories
     */
    private boolean isMarketplaceInventory(String title) {
        if (title == null) return false;
        
        // Define exact marketplace GUI titles to avoid false positives
        return title.contains("MarketPlace") || 
               title.contains("Black Market") || 
               title.contains("My Items") || 
               title.contains("Transaction History") ||
               title.contains("Transactions") ||
               title.contains("Confirm Purchase") || 
               title.contains("§eConfirm Purchase") ||
               title.contains("§6MarketPlace") ||
               title.contains("§4Black Market") ||
               title.contains("§6My Items") ||
               title.contains("§7Transaction History") ||
               title.contains("§9Transaction History") ||
               title.contains("Admin Panel") ||
               title.contains("§cAdmin Panel");
    }
    
    /**
     * SMART ANTI-THEFT SYSTEM
     * Detect unauthorized item manipulation attempts while allowing legitimate GUI navigation
     */
    private boolean isUnauthorizedItemManipulation(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ClickType clickType = event.getClick();
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Always block these dangerous operations
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            return true; // Shift-click can move items between inventories
        }
        
        if (clickType == ClickType.NUMBER_KEY) {
            return true; // Number keys swap items with hotbar
        }
        
        if (clickType == ClickType.MIDDLE && player.getGameMode() == GameMode.CREATIVE) {
            return true; // Middle click in creative can duplicate items
        }
        
        if (clickType == ClickType.DOUBLE_CLICK) {
            return true; // Double-click collects similar items
        }
        
        if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
            return true; // Drop operations
        }
        
        if (clickType == ClickType.CREATIVE && player.getGameMode() == GameMode.CREATIVE) {
            return true; // Creative mode item manipulation
        }
        
        // Block attempts to place items from cursor into GUI
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            return true; // Player has item on cursor trying to place it
        }
        
        // Block unknown click types that might be exploits
        if (clickType == ClickType.UNKNOWN) {
            return true; // Unknown click types could be exploits
        }
        
        // SPECIAL HANDLING: For Transaction History GUIs, block everything except navigation
        String title = event.getView().getTitle();
        if (title.contains("Transaction History") || title.contains("Transactions")) {
            // Transaction history is read-only except for navigation buttons
            // Let the specific handler deal with button detection
            return false; // Let the handler deal with it
        }
        
        // Only block pickup attempts when player is trying to steal items
        // Normal left-click for purchasing/navigation should be allowed
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
            // These are legitimate interactions for purchasing items or navigation
            return false;
        }
        
        return false;
    }
    
    /**
     * Handle unauthorized actions with appropriate security measures
     */
    private void handleUnauthorizedAction(Player player, InventoryClickEvent event) {
        // Clear cursor to prevent any item duplication or loss
        if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
            event.setCursor(null);
        }
        
        // Update player inventory to ensure consistency
        player.updateInventory();
    }
    
    /**
     * Create appropriate GUI based on inventory title
     */
    private Object createGUIFromTitle(String title, Player player) {
        if (title.contains("MarketPlace")) {
            return new MarketplaceGUI(plugin, player);
        } else if (title.contains("Black Market")) {
            return new BlackMarketGUI(plugin, player);
        } else if (title.contains("My Items")) {
            return new MyItemsGUI(plugin, player);
        } else if (title.contains("Transaction History")) {
            return new TransactionHistoryGUI(plugin, player);
        } else if (title.contains("Admin Panel")) {
            return new AdminGUI(plugin, player);
        }
        return null;
    }

    /**
     * Handle marketplace GUI clicks
     */
    private void handleMarketplaceClick(InventoryClickEvent event, Player player, MarketplaceGUI gui, ItemStack clickedItem) {
        int slot = event.getSlot();

        // Check if this is a GUI button using NBT
        String buttonType = gui.getButtonType(slot);
        if (buttonType != null) {
            switch (buttonType) {
                case "next-page":
                    gui.nextPage();
                    break;
                case "previous-page":
                    gui.previousPage();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "my-items":
                    // Simple approach: close current inventory and open My Items directly
                    player.closeInventory();
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        MyItemsGUI myItemsGUI = new MyItemsGUI(plugin, player);
                        plugin.getGUIManager().registerGUI(player.getUniqueId(), myItemsGUI);
                        
                        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                            plugin.getLogger().info("Opening My Items GUI for player " + player.getName());
                        }
                        
                        myItemsGUI.open();
                    }, 1L);
                    break;
                case "page-info":
                    // Do nothing for page info button
                    break;
            }
        } else {
            // Handle market item clicks
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
        int slot = event.getSlot();

        // Check if this is a GUI button using NBT
        String buttonType = gui.getButtonType(slot);
        if (buttonType != null) {
            switch (buttonType) {
                case "next-page":
                    gui.nextPage();
                    break;
                case "previous-page":
                    gui.previousPage();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "my-items":
                    // Simple approach: close current inventory and open My Items directly
                    player.closeInventory();
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        MyItemsGUI myItemsGUI = new MyItemsGUI(plugin, player);
                        plugin.getGUIManager().registerGUI(player.getUniqueId(), myItemsGUI);
                        
                        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                            plugin.getLogger().info("Opening My Items GUI for player " + player.getName());
                        }
                        
                        myItemsGUI.open();
                    }, 1L);
                    break;
                case "info":
                case "page-info":
                    // Do nothing for info buttons
                    break;
            }
        } else {
            // Handle market item clicks
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
        int slot = event.getSlot();

        // Check if this is a GUI button using NBT
        String buttonType = gui.getButtonType(slot);
        if (buttonType != null) {
            switch (buttonType) {
                case "next-page":
                    gui.nextPage();
                    break;
                case "previous-page":
                    gui.previousPage();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "back":
                    // Simple approach: close current inventory and open marketplace
                    player.closeInventory();
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        MarketplaceGUI marketplaceGUI = new MarketplaceGUI(plugin, player);
                        plugin.getGUIManager().registerGUI(player.getUniqueId(), marketplaceGUI);
                        
                        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                            plugin.getLogger().info("Returning to Marketplace from My Items for player " + player.getName());
                        }
                        
                        marketplaceGUI.open();
                    }, 1L);
                    break;
                case "page-info":
                    // Do nothing for page info button
                    break;
            }
        } else {
            // Handle item removal
            MarketItem item = gui.getMarketItemAtSlot(slot);
            if (item != null) {
                gui.removeItem(item); // removeItem method already sends the message
            }
        }
    }

    /**
     * Handle transaction history GUI clicks
     */
    private void handleTransactionHistoryClick(InventoryClickEvent event, Player player, TransactionHistoryGUI gui, ItemStack clickedItem) {
        int slot = event.getSlot();

        // ALWAYS cancel the event first - transaction history is read-only
        event.setCancelled(true);
        
        // Clear cursor to prevent any item duplication attempts
        if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
            event.setCursor(null);
        }
        
        // Update inventory to ensure consistency
        player.updateInventory();

        // Only allow navigation button clicks
        String buttonType = gui.getButtonType(slot);
        if (buttonType != null) {
            switch (buttonType) {
                case "next-page":
                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("Transaction history next page clicked by " + player.getName());
                    }
                    gui.nextPage();
                    break;
                case "previous-page":
                    if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                        plugin.getLogger().info("Transaction history previous page clicked by " + player.getName());
                    }
                    gui.previousPage();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "page-info":
                    // Do nothing for page info button
                    break;
                default:
                    // Unknown button type - ignore
                    break;
            }
        }
        
        // All other clicks are completely blocked for security
        // Transaction history is purely informational and read-only
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
            // Simple approach: close current inventory and open confirmation
            player.closeInventory();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ConfirmationGUI confirmationGUI = new ConfirmationGUI(plugin, player, item, isBlackMarket);
                plugin.getGUIManager().registerGUI(player.getUniqueId(), confirmationGUI);
                
                if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                    plugin.getLogger().info("Opening confirmation GUI for player " + player.getName());
                }
                
                confirmationGUI.open();
            }, 2L);
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

        // Add debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("Confirmation click for player " + player.getName() + " - slot: " + slot + " - title: " + event.getView().getTitle());
        }

        // Find the confirmation GUI
        Object gui = plugin.getGUIManager().getGUI(player.getUniqueId());
        if (!(gui instanceof ConfirmationGUI)) {
            // If GUI is not found in manager, try to check if the clicked inventory is a confirmation GUI
            // This can happen if the GUI registration failed or was cleared
            String title = event.getView().getTitle();
            if (!title.contains("Confirm Purchase") && !title.contains("§eConfirm Purchase")) {
                if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                    plugin.getLogger().warning("Confirmation GUI not found for player " + player.getName() + " and title doesn't match confirmation GUI. Title: '" + title + "', GUI type: " + (gui != null ? gui.getClass().getSimpleName() : "null"));
                }
                return;
            }
            // If title matches but GUI not registered, recreate the GUI registration
            // This is a fallback for cases where GUI registration was lost
            if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
                plugin.getLogger().info("Confirmation GUI title detected but not properly registered for player " + player.getName() + ", attempting to recover");
            }
            
            // Try to find the associated item by checking the inventory content
            // This is a fallback mechanism if the GUI registration is lost
            player.closeInventory();
            return;
        }

        ConfirmationGUI confirmationGUI = (ConfirmationGUI) gui;

        // Get confirm and cancel slots from config
        int confirmSlot = 11; // default
        int cancelSlot = 15; // default
        
        try {
            var guiConfig = plugin.getConfigManager().getGuiConfig().getConfigurationSection("confirmation");
            if (guiConfig != null) {
                confirmSlot = guiConfig.getInt("items.confirm.slot", 11);
                cancelSlot = guiConfig.getInt("items.cancel.slot", 15);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading confirmation GUI config: " + e.getMessage());
        }

        // Check by slot instead of display name to avoid issues with color codes and translations
        if (slot == confirmSlot) { // Confirm button slot
            MarketItem item = confirmationGUI.getMarketItem();
            boolean isBlackMarket = confirmationGUI.isBlackMarket();

            // Unregister the confirmation GUI first to prevent conflicts
            plugin.getGUIManager().unregisterGUI(player.getUniqueId());
            player.closeInventory();
            
            // Process purchase after a small delay to ensure inventory is closed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                processPurchase(player, item, isBlackMarket);
            }, 1L);

        } else if (slot == cancelSlot) { // Cancel button slot
            // Unregister the confirmation GUI
            plugin.getGUIManager().unregisterGUI(player.getUniqueId());
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
                player.sendMessage(plugin.getConfigManager().getMessage("marketplace.inventory-full"));
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
                
                // Get proper item display name instead of raw serialized data
                String itemDisplayName;
                try {
                    ItemStack itemStack = ItemSerializer.deserializeItemStack(currentItem.getItemData());
                    itemDisplayName = ItemSerializer.getDisplayName(itemStack);
                } catch (Exception e) {
                    itemDisplayName = "Unknown Item";
                    plugin.getLogger().warning("Failed to deserialize item for display name: " + e.getMessage());
                }
                
                seller.sendMessage(plugin.getConfigManager().getMessage("marketplace.item-sold",
                    "item", itemDisplayName,
                    "buyer", player.getName(),
                    "price", plugin.getEconomyManager().formatMoney(sellerPayment)));
            } else {
                // Seller is offline - could implement offline payment system
                // For now, just deposit to their account when they come online
                // TODO: Implement offline payment system
                // For now, log the payment for later processing
                plugin.getLogger().info("Offline payment pending for " + currentItem.getSellerName() + ": " + sellerPayment);
            }

            // Give item to buyer - clean it from marketplace metadata
            ItemStack itemStack = ItemSerializer.deserializeItemStack(currentItem.getItemData());
            ItemStack cleanItem = cleanItemForPlayerInventory(itemStack);
            player.getInventory().addItem(cleanItem);

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
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        // Only handle marketplace GUI closes
        if (!isMarketplaceInventory(title)) {
            return;
        }
        
        // Clean up registered GUI when marketplace inventory is closed
        // This prevents memory leaks and ensures clean state
        plugin.getGUIManager().unregisterGUI(player.getUniqueId());
        
        // Add debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("Marketplace inventory closed - Player: " + player.getName() + ", Title: " + title);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Only handle player drags
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Block ALL drag operations in marketplace GUIs
        if (isMarketplaceInventory(title)) {
            event.setCancelled(true);
            
            // Clear cursor to prevent item duplication
            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                event.setCursor(null);
            }
            
            // Update inventory to ensure consistency
            player.updateInventory();
        }
    }
    
    /**
     * Block item drops while marketplace GUI is open
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has a marketplace GUI open
        if (plugin.getGUIManager().getGUI(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block item pickup while marketplace GUI is open to prevent inventory manipulation
     */
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has a marketplace GUI open
        if (plugin.getGUIManager().getGUI(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block automated item movement while marketplace GUI is open
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Block any automated item movement involving marketplace GUIs
        if (isMarketplaceInventory(event.getDestination().getType().name()) || 
            isMarketplaceInventory(event.getSource().getType().name())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle admin GUI clicks
     */
    private void handleAdminClick(InventoryClickEvent event, Player player, AdminGUI gui, ItemStack clickedItem) {
        int slot = event.getSlot();

        // Check if this is a GUI button using NBT
        String buttonType = gui.getButtonType(slot);
        if (buttonType != null) {
            switch (buttonType) {
                case "next-page":
                    gui.nextPage();
                    break;
                case "previous-page":
                    gui.previousPage();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "page-info":
                    // Do nothing for page info button
                    break;
            }
        } else {
            // Handle market item clicks
            String itemId = gui.getMarketItemId(slot);
            if (itemId != null) {
                ClickType clickType = event.getClick();
                
                if (clickType == ClickType.LEFT) {
                    // Left click: Return item to seller
                    gui.returnItem(itemId);
                } else if (clickType == ClickType.RIGHT) {
                    // Right click: Confiscate item
                    gui.confiscateItem(itemId);
                }
            }
        }
    }

    /**
     * Clean item from marketplace metadata for normal player inventory use
     * This ensures the item behaves normally in the player's inventory
     */
    private ItemStack cleanItemForPlayerInventory(ItemStack item) {
        if (item == null) return null;
        
        // Create a completely new ItemStack to avoid any lingering metadata
        ItemStack cleanItem = new ItemStack(item.getType(), item.getAmount());
        
        // Copy only the essential metadata (display name, lore, enchantments, etc.)
        if (item.hasItemMeta()) {
            ItemMeta originalMeta = item.getItemMeta();
            ItemMeta cleanMeta = cleanItem.getItemMeta();
            
            if (originalMeta != null && cleanMeta != null) {
                // Copy display name
                if (originalMeta.hasDisplayName()) {
                    cleanMeta.setDisplayName(originalMeta.getDisplayName());
                }
                
                // Copy lore
                if (originalMeta.hasLore()) {
                    cleanMeta.setLore(originalMeta.getLore());
                }
                
                // Copy enchantments
                if (originalMeta.hasEnchants()) {
                    cleanMeta.getEnchants().forEach((enchant, level) -> {
                        cleanMeta.addEnchant(enchant, level, true);
                    });
                }
                
                // Copy item flags
                cleanMeta.addItemFlags(originalMeta.getItemFlags().toArray(new org.bukkit.inventory.ItemFlag[0]));
                
                // Copy custom model data
                if (originalMeta.hasCustomModelData()) {
                    cleanMeta.setCustomModelData(originalMeta.getCustomModelData());
                }
                
                // Apply the clean metadata
                cleanItem.setItemMeta(cleanMeta);
            }
        }
        
        // The item should now behave exactly like a normal item in the player's inventory
        // with no marketplace-specific metadata or NBT data
        return cleanItem;
    }
}