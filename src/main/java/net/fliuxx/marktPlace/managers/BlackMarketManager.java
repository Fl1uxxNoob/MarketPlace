package net.fliuxx.marktPlace.managers;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.database.models.TimerState;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Black Market Manager
 * Handles black market operations and automatic refreshing with MongoDB persistence
 */
public class BlackMarketManager {

    private final MarktPlace plugin;
    private BukkitTask refreshTask;
    private long lastRefreshTime;
    private long nextRefreshTime;
    private TimerState timerState;
    private final Object lock = new Object();
    private static final String TIMER_ID = "blackmarket_timer";

    public BlackMarketManager(MarktPlace plugin) {
        this.plugin = plugin;

        // Load timer state from database
        this.timerState = plugin.getMongoManager().loadTimerState(TIMER_ID);
        this.lastRefreshTime = timerState.getLastRefreshTime();
        this.nextRefreshTime = timerState.getNextRefreshTime();

        // Removed excessive logging during initialization
    }

    /**
     * Start the automatic refresh task
     */
    public void startRefreshTask() {
        // Stop any existing tasks first
        stopRefreshTask();

        if (!plugin.getConfig().getBoolean("blackmarket.auto-refresh", true)) {
            plugin.getLogger().info("Black market auto-refresh is disabled");
            return;
        }

        long refreshIntervalSeconds = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400);
        long refreshIntervalMs = refreshIntervalSeconds * 1000L;
        long currentTime = System.currentTimeMillis();

        long delayUntilNextRefresh;
        boolean isResuming = false;

        // Check if we have a valid next refresh time from database
        if (nextRefreshTime > 0 && nextRefreshTime > currentTime) {
            // Resume from saved state
            delayUntilNextRefresh = nextRefreshTime - currentTime;
            isResuming = true;
            // Removed excessive debugging logs
        } else {
            // Calculate new refresh time or do immediate refresh if expired
            long timeSinceLastRefresh = currentTime - lastRefreshTime;
            delayUntilNextRefresh = refreshIntervalMs - timeSinceLastRefresh;

            if (delayUntilNextRefresh <= 0) {
                refreshBlackMarket();
                delayUntilNextRefresh = refreshIntervalMs;
            }

            nextRefreshTime = currentTime + delayUntilNextRefresh;
            // Log only essential information
            plugin.getLogger().info("Black market timer set for " + (delayUntilNextRefresh / 1000 / 60) + " minutes");

            // Save ONLY when we calculate a new timer state - but NOT when resuming
            saveTimerState();
        }

        // Convert delay to ticks
        long initialDelay = delayUntilNextRefresh / 50; // Convert ms to ticks
        long intervalTicks = refreshIntervalMs / 50;

        // Main timer - executes refresh at exact interval
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("blackmarket.auto-refresh", true)) {
                    plugin.getLogger().info("Auto-refresh disabled, stopping timer");
                    this.cancel();
                    return;
                }

                long actualRefreshTime = System.currentTimeMillis();
                plugin.getLogger().info("Black market auto-refresh triggered");

                refreshBlackMarket();

                // Update next refresh time and save to database
                nextRefreshTime = actualRefreshTime + refreshIntervalMs;
                saveTimerState();

                // Broadcast refresh message
                String message = plugin.getConfigManager().getMessage("blackmarket.auto-refresh");
                Bukkit.broadcastMessage(message);
            }
        }.runTaskTimer(plugin, initialDelay, intervalTicks);

        // Periodic save timer - saves state every 5 minutes
        // REMOVED: This was causing timer drift by constantly saving and recalculating
        // saveTask = new BukkitRunnable() {
        //     @Override
        //     public void run() {
        //         saveTimerState();
        //     }
        // }.runTaskTimer(plugin, 6000L, 6000L);

        plugin.getLogger().info("Black market auto-refresh started");
    }

    /**
     * Stop the automatic refresh task
     */
    public void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        // saveTask removed - was causing timer drift

        // CRITICAL: Just save the current nextRefreshTime without recalculating
        // The Bukkit scheduler should maintain the correct timing

        // Save timer state before stopping - with the ORIGINAL nextRefreshTime
        saveTimerState();
    }

    /**
     * Reload refresh task based on current configuration
     */
    public void reloadRefreshTask() {
        stopRefreshTask();
        startRefreshTask();
    }

    /**
     * Refresh the black market
     */
    public void refreshBlackMarket() {
        synchronized (lock) {
            try {
                // Move unsold black market items back to regular market
                plugin.getMongoManager().moveBlackMarketItemsToMarket();

                // Clear existing black market items
                plugin.getMongoManager().clearBlackMarket();

                // Get all market items
                List<MarketItem> marketItems = plugin.getMongoManager().getAllMarketItems();

                if (marketItems.isEmpty()) {
                    plugin.getLogger().info("No items available for black market refresh");
                    return;
                }

                // Calculate the number of items to select: market items / 2, rounded up
                int itemsToSelect = (int) Math.ceil(marketItems.size() / 2.0);

                // Shuffle the list for random selection
                Collections.shuffle(marketItems);

                // Select the first itemsToSelect items
                List<MarketItem> selectedItems = marketItems.subList(0, Math.min(itemsToSelect, marketItems.size()));

                // Apply black market discount and move items
                double discountPercentage = plugin.getConfig().getDouble("blackmarket.discount-percentage", 30.0);

                for (MarketItem item : selectedItems) {
                    // Calculate discounted price
                    double discountedPrice = item.getPrice() * (1 - discountPercentage / 100.0);

                    // Store original price for seller calculation
                    item.setOriginalPrice(item.getPrice());
                    item.setPrice(discountedPrice);

                    // Move to black market
                    plugin.getMongoManager().moveItemToBlackMarket(item);
                }

                // Update last refresh time
                lastRefreshTime = System.currentTimeMillis();

                plugin.getLogger().info("Black market refreshed with " + selectedItems.size() + " items");
                
                // Refresh all GUIs after black market refresh
                plugin.getGUIManager().refreshBlackMarketGUIs();

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
        return plugin.getMongoManager().getBlackMarketItems();
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
        if (nextRefreshTime == 0) {
            // If no next refresh time set, calculate based on last refresh
            long refreshInterval = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400) * 1000L;
            long timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime;
            return Math.max(0, refreshInterval - timeSinceLastRefresh);
        }

        // Use the scheduled next refresh time
        return Math.max(0, nextRefreshTime - System.currentTimeMillis());
    }

    /**
     * Get formatted time until next refresh
     */
    public String getFormattedTimeUntilNextRefresh() {
        // If auto-refresh is disabled, show disabled status
        if (!plugin.getConfig().getBoolean("blackmarket.auto-refresh", true)) {
            return "Disabled";
        }

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
     * Force refresh black market and restart automatic timer
     */
    public void forceRefresh() {
        plugin.getLogger().info("Manual black market refresh triggered");

        // Perform the refresh
        refreshBlackMarket();

        // Reset the automatic timer to start from now
        long refreshIntervalSeconds = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400);
        long refreshIntervalMs = refreshIntervalSeconds * 1000L;
        long currentTime = System.currentTimeMillis();

        // Update timer variables
        lastRefreshTime = currentTime;
        nextRefreshTime = currentTime + refreshIntervalMs;

        // Save the new timer state
        saveTimerState();

        // Restart the automatic refresh task with new timing
        if (plugin.getConfig().getBoolean("blackmarket.auto-refresh", true)) {
            startRefreshTask();
        }
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

    /**
     * Save timer state to database - ONLY call when timer state actually changes
     */
    private void saveTimerState() {
        try {
            timerState.setLastRefreshTime(lastRefreshTime);
            timerState.setNextRefreshTime(nextRefreshTime);
            plugin.getMongoManager().saveTimerState(timerState);

            // Precise debug log to track exact times
            long currentTime = System.currentTimeMillis();
            long remaining = nextRefreshTime - currentTime;
            plugin.getLogger().info("SAVE: Timer state saved - nextRefresh=" + nextRefreshTime + 
                ", lastRefresh=" + lastRefreshTime + ", current=" + currentTime + ", remaining=" + remaining/1000 + "s");
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving timer state: " + e.getMessage());
        }
    }
}