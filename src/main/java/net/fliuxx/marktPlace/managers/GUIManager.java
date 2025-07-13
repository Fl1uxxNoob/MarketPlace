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
     * Refresh all "My Items" GUIs
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
     * Get all open GUIs count
     */
    public int getOpenGUICount() {
        return openGuis.size();
    }
}