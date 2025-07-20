package net.fliuxx.marktPlace.commands;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.gui.BlackMarketGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Black Market Command
 * Handles /blackmarket and /blackmarketrefresh commands
 */
public class BlackMarketCommand implements CommandExecutor, TabCompleter {

    private final MarktPlace plugin;

    public BlackMarketCommand(MarktPlace plugin) {
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

        // Handle blackmarketrefresh command
        if (label.equalsIgnoreCase("blackmarketrefresh") || label.equalsIgnoreCase("bmrefresh") || 
            label.equalsIgnoreCase("refreshbm")) {
            return handleRefresh(player);
        }

        // Handle blackmarket command
        if (label.equalsIgnoreCase("blackmarket") || label.equalsIgnoreCase("bm") || 
            label.equalsIgnoreCase("blackm")) {
            return handleBlackMarket(player, args);
        }

        return false;
    }

    /**
     * Handle black market GUI opening
     */
    private boolean handleBlackMarket(Player player, String[] args) {
        // Check permission
        if (!player.hasPermission("marketplace.blackmarket")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "refresh":
                    return handleRefresh(player);
                case "stats":
                    return handleStats(player);
                case "help":
                    return handleHelp(player);
                default:
                    player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                    return true;
            }
        }

        // Open black market GUI
        try {
            BlackMarketGUI gui = new BlackMarketGUI(plugin, player);
            gui.open();
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error opening black market GUI for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle refresh subcommand
     */
    private boolean handleRefresh(Player player) {
        // Check permission
        if (!player.hasPermission("marketplace.blackmarket.refresh")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        try {
            plugin.getBlackMarketManager().forceRefresh();
            player.sendMessage(plugin.getConfigManager().getMessage("blackmarket.refreshed"));
            
            // Show additional info about timer restart
            long refreshInterval = plugin.getConfig().getLong("blackmarket.refresh-interval", 86400);
            String timeUnit = refreshInterval >= 3600 ? (refreshInterval / 3600) + " hours" : (refreshInterval / 60) + " minutes";
            player.sendMessage("§7Automatic timer restarted - next refresh in §e" + timeUnit);
            
            // Log the refresh
            plugin.getLogger().info("Black market manually refreshed by " + player.getName());
            
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error refreshing black market: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle stats subcommand
     */
    private boolean handleStats(Player player) {
        // Check permission
        if (!player.hasPermission("marketplace.blackmarket")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        try {
            var stats = plugin.getBlackMarketManager().getStatistics();
            String prefix = plugin.getConfigManager().getMessage("prefix");
            
            player.sendMessage(prefix + "§6Black Market Statistics:");
            player.sendMessage("§7Items Available: §e" + stats.getItemCount());
            player.sendMessage("§7Total Value: §6" + plugin.getEconomyManager().formatMoney(stats.getTotalValue()));
            player.sendMessage("§7Original Value: §6" + plugin.getEconomyManager().formatMoney(stats.getTotalOriginalValue()));
            player.sendMessage("§7Total Savings: §a" + plugin.getEconomyManager().formatMoney(stats.getTotalSavings()));
            player.sendMessage("§7Next Refresh: §e" + plugin.getBlackMarketManager().getFormattedTimeUntilNextRefresh());
            
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error getting black market stats: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle help subcommand
     */
    private boolean handleHelp(Player player) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        
        player.sendMessage(prefix + "§4Black Market Help:");
        player.sendMessage("§7/blackmarket §8- §eOpen the black market");
        player.sendMessage("§7/blackmarket stats §8- §eView black market statistics");
        
        if (player.hasPermission("marketplace.blackmarket.refresh")) {
            player.sendMessage("§7/blackmarket refresh §8- §eRefresh black market");
            player.sendMessage("§7/blackmarketrefresh §8- §eRefresh black market");
        }
        
        player.sendMessage("");
        player.sendMessage("§7Black Market Info:");
        player.sendMessage("§7• All items have §c50% §7discount");
        player.sendMessage("§7• Sellers get §a2x §7profit when sold");
        player.sendMessage("§7• Refreshes every §e24 hours");
        player.sendMessage("§7• Max §e" + plugin.getConfig().getInt("blackmarket.max-items", 27) + " §7items");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("stats");
            completions.add("help");
            
            if (sender.hasPermission("marketplace.blackmarket.refresh")) {
                completions.add("refresh");
            }
        }

        return completions;
    }
}
