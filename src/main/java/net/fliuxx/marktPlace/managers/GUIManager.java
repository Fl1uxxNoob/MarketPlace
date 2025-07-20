package net.fliuxx.marktPlace.managers;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.gui.BlackMarketGUI;
import net.fliuxx.marktPlace.gui.MarketplaceGUI;
import net.fliuxx.marktPlace.gui.MyItemsGUI;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI Manager for MarketPlace Plugin
 * Manages all open GUIs and provides auto-refresh functionality
 */
public class GUIManager {

    private final MarktPlace plugin;
    private final Map<UUID, Object> openGuis = new HashMap<>();

    public GUIManager(MarktPlace plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a GUI for a player
     */
    public void registerGUI(UUID playerId, Object gui) {
        openGuis.put(playerId, gui);
    }

    /**
     * Unregister a GUI for a player
     */
    public void unregisterGUI(UUID playerId) {
        openGuis.remove(playerId);
    }

    /**
     * Get the GUI for a player
     */
    public Object getGUI(UUID playerId) {
        return openGuis.get(playerId);
    }

    /**
     * Refresh all marketplace GUIs
     */
    public void refreshMarketplaceGUIs() {
        for (Map.Entry<UUID, Object> entry : openGuis.entrySet()) {
            if (entry.getValue() instanceof MarketplaceGUI) {
                MarketplaceGUI gui = (MarketplaceGUI) entry.getValue();
                gui.refresh();
            }
        }
    }

    /**
     * Refresh all black market GUIs
     */
    public void refreshBlackMarketGUIs() {
        for (Map.Entry<UUID, Object> entry : openGuis.entrySet()) {
            if (entry.getValue() instanceof BlackMarketGUI) {
                BlackMarketGUI gui = (BlackMarketGUI) entry.getValue();
                gui.refresh();
            }
        }
    }

    /**
     * Refresh all my items GUIs
     */
    public void refreshMyItemsGUIs() {
        for (Map.Entry<UUID, Object> entry : openGuis.entrySet()) {
            if (entry.getValue() instanceof MyItemsGUI) {
                MyItemsGUI gui = (MyItemsGUI) entry.getValue();
                gui.refresh();
            }
        }
    }

    /**
     * Refresh all GUIs
     */
    public void refreshAllGUIs() {
        refreshMarketplaceGUIs();
        refreshBlackMarketGUIs();
        refreshMyItemsGUIs();
    }

    /**
     * Clear all registered GUIs
     */
    public void clearAll() {
        openGuis.clear();
    }

    /**
     * Check if a player has a GUI open
     */
    public boolean hasGUIOpen(UUID playerId) {
        return openGuis.containsKey(playerId);
    }

    /**
     * Switch to a secondary GUI while keeping a fallback to main marketplace
     */
    public void openSecondaryGUI(UUID playerId, Object secondaryGUI) {
        // Register the secondary GUI
        registerGUI(playerId, secondaryGUI);
        
        // Add debug logging
        if (plugin.getConfig().getBoolean("debug.gui-debugging", false)) {
            plugin.getLogger().info("Opening secondary GUI for player " + playerId + " - GUI type: " + secondaryGUI.getClass().getSimpleName());
        }
    }

    /**
     * Get the appropriate GUI type for a player based on their current state
     */
    public Object getOrCreateGUI(UUID playerId, Player player, String preferredType) {
        Object existingGUI = getGUI(playerId);
        
        // If no GUI exists or wrong type, create appropriate one
        if (existingGUI == null || !existingGUI.getClass().getSimpleName().toLowerCase().contains(preferredType.toLowerCase())) {
            Object newGUI;
            
            switch (preferredType.toLowerCase()) {
                case "marketplace":
                    newGUI = new MarketplaceGUI(plugin, player);
                    break;
                case "blackmarket":
                    newGUI = new BlackMarketGUI(plugin, player);
                    break;
                case "myitems":
                    newGUI = new MyItemsGUI(plugin, player);
                    break;
                default:
                    newGUI = new MarketplaceGUI(plugin, player); // Default fallback
                    break;
            }
            
            registerGUI(playerId, newGUI);
            return newGUI;
        }
        
        return existingGUI;
    }

    /**
     * Get all open GUIs count
     */
    public int getOpenGUICount() {
        return openGuis.size();
    }
}