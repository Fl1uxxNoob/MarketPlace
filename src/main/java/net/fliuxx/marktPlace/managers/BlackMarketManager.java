package net.fliuxx.marktPlace.managers;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Black Market Manager
 * Handles black market operations and automatic refreshing
 */
public class BlackMarketManager {

    private final MarktPlace plugin;
    private BukkitTask refreshTask;
    private long lastRefreshTime;
    private final Object lock = new Object();

    public BlackMarketManager(MarktPlace plugin) {
        this.plugin = plugin;
        this.lastRefreshTime = System.currentTimeMillis();
    }

    /**
     * Start the automatic refresh task
     */
    public void startRefreshTask() {
        if (!plugin.getConfig().getBoolean("blackmarket.auto-refresh", true)) {
            return;
        }

        long refreshInterval = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400) * 20L; // Convert to ticks

        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshBlackMarket();
                
                // Broadcast refresh message
                String message = plugin.getConfigManager().getMessage("blackmarket.auto-refresh");
                Bukkit.broadcastMessage(message);
            }
        }.runTaskTimer(plugin, refreshInterval, refreshInterval);

        plugin.getLogger().info("Black market auto-refresh started with interval: " + (refreshInterval / 20) + " seconds");
    }

    /**
     * Stop the automatic refresh task
     */
    public void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /**
     * Refresh the black market
     */
    public void refreshBlackMarket() {
        synchronized (lock) {
            try {
                // Clear existing black market items
                plugin.getMongoManager().clearBlackMarket();

                // Get all market items
                List<MarketItem> marketItems = plugin.getMongoManager().getAllMarketItems();
                
                if (marketItems.isEmpty()) {
                    plugin.getLogger().info("No items available for black market refresh");
                    return;
                }

                // Shuffle and select random items
                Collections.shuffle(marketItems);
                
                int maxItems = plugin.getConfig().getInt("blackmarket.max-items", 27);
                int itemCount = Math.min(maxItems, marketItems.size());
                
                List<MarketItem> selectedItems = new ArrayList<>();
                for (int i = 0; i < itemCount; i++) {
                    selectedItems.add(marketItems.get(i));
                }

                // Apply black market modifications and move items
                double discountPercentage = plugin.getConfig().getDouble("blackmarket.discount-percentage", 50.0);
                
                for (MarketItem item : selectedItems) {
                    // Remove from regular market
                    plugin.getMongoManager().removeMarketItem(item.getId());
                    
                    // Create black market version
                    MarketItem blackMarketItem = new MarketItem(
                        item.getId(),
                        item.getSellerId(),
                        item.getSellerName(),
                        item.getItemStack(),
                        item.getItemData(),
                        item.getPrice() * (1.0 - discountPercentage / 100.0), // Apply discount
                        item.getListedAt(),
                        true, // Mark as black market item
                        item.getPrice() // Store original price
                    );
                    
                    // Add to black market
                    plugin.getMongoManager().addBlackMarketItem(blackMarketItem);
                }

                lastRefreshTime = System.currentTimeMillis();
                plugin.getLogger().info("Black market refreshed with " + selectedItems.size() + " items");

            } catch (Exception e) {
                plugin.getLogger().severe("Error refreshing black market: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all black market items
     */
    public List<MarketItem> getBlackMarketItems() {
        return plugin.getMongoManager().getAllBlackMarketItems();
    }

    /**
     * Get black market item by ID
     */
    public MarketItem getBlackMarketItem(String itemId) {
        return plugin.getMongoManager().getBlackMarketItem(itemId);
    }

    /**
     * Remove item from black market
     */
    public void removeBlackMarketItem(String itemId) {
        plugin.getMongoManager().removeBlackMarketItem(itemId);
    }

    /**
     * Calculate seller profit for black market sale
     */
    public double calculateSellerProfit(double originalPrice) {
        double multiplier = plugin.getConfig().getDouble("blackmarket.seller-multiplier", 2.0);
        return originalPrice * multiplier;
    }

    /**
     * Get time until next refresh
     */
    public long getTimeUntilNextRefresh() {
        long refreshInterval = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400) * 1000L; // Convert to milliseconds
        long timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime;
        return Math.max(0, refreshInterval - timeSinceLastRefresh);
    }

    /**
     * Get formatted time until next refresh
     */
    public String getFormattedTimeUntilNextRefresh() {
        long timeLeft = getTimeUntilNextRefresh();
        
        if (timeLeft <= 0) {
            return "Ready to refresh";
        }
        
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Check if it's time for automatic refresh
     */
    public boolean isTimeForRefresh() {
        long refreshInterval = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400) * 1000L;
        return System.currentTimeMillis() - lastRefreshTime >= refreshInterval;
    }

    /**
     * Force refresh black market
     */
    public void forceRefresh() {
        refreshBlackMarket();
    }

    /**
     * Get black market statistics
     */
    public BlackMarketStats getStatistics() {
        List<MarketItem> items = getBlackMarketItems();
        
        double totalValue = 0.0;
        double totalOriginalValue = 0.0;
        
        for (MarketItem item : items) {
            totalValue += item.getPrice();
            totalOriginalValue += item.getOriginalPrice();
        }
        
        double totalSavings = totalOriginalValue - totalValue;
        
        return new BlackMarketStats(
            items.size(),
            totalValue,
            totalOriginalValue,
            totalSavings,
            lastRefreshTime,
            getTimeUntilNextRefresh()
        );
    }

    /**
     * Black Market Statistics class
     */
    public static class BlackMarketStats {
        private final int itemCount;
        private final double totalValue;
        private final double totalOriginalValue;
        private final double totalSavings;
        private final long lastRefreshTime;
        private final long timeUntilNextRefresh;

        public BlackMarketStats(int itemCount, double totalValue, double totalOriginalValue, 
                               double totalSavings, long lastRefreshTime, long timeUntilNextRefresh) {
            this.itemCount = itemCount;
            this.totalValue = totalValue;
            this.totalOriginalValue = totalOriginalValue;
            this.totalSavings = totalSavings;
            this.lastRefreshTime = lastRefreshTime;
            this.timeUntilNextRefresh = timeUntilNextRefresh;
        }

        // Getters
        public int getItemCount() { return itemCount; }
        public double getTotalValue() { return totalValue; }
        public double getTotalOriginalValue() { return totalOriginalValue; }
        public double getTotalSavings() { return totalSavings; }
        public long getLastRefreshTime() { return lastRefreshTime; }
        public long getTimeUntilNextRefresh() { return timeUntilNextRefresh; }
    }
}
