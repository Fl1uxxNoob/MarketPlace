package net.fliuxx.marktPlace.commands;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.gui.TransactionHistoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Transactions Command
 * Handles /transactions command for viewing transaction history
 */
public class TransactionsCommand implements CommandExecutor, TabCompleter {

    private final MarktPlace plugin;

    public TransactionsCommand(MarktPlace plugin) {
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
        if (!player.hasPermission("marketplace.history")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "clear":
                    return handleClear(player);
                case "stats":
                    return handleStats(player);
                case "help":
                    return handleHelp(player);
                default:
                    // Check if this is a player name for viewing other's transactions
                    if (player.hasPermission("marketplace.history.other")) {
                        return handlePlayerTransactions(player, args[0]);
                    }
                    player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                    return true;
            }
        }

        // Open transaction history GUI
        try {
            TransactionHistoryGUI gui = new TransactionHistoryGUI(plugin, player);
            gui.open();
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error opening transaction history GUI for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle clear subcommand (admin only)
     */
    private boolean handleClear(Player player) {
        if (!player.hasPermission("marketplace.admin.clear")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // This would require additional implementation to clear transaction history
        // For now, just inform the player
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§cTransaction clearing is not yet implemented.");
        return true;
    }

    /**
     * Handle stats subcommand
     */
    private boolean handleStats(Player player) {
        try {
            var playerData = plugin.getMongoManager().getPlayerData(player.getUniqueId());
            var transactions = plugin.getMongoManager().getTransactionsByPlayer(player.getUniqueId());
            
            String prefix = plugin.getConfigManager().getMessage("prefix");
            
            player.sendMessage(prefix + "§9Your Transaction Statistics:");
            player.sendMessage("§7Total Transactions: §e" + transactions.size());
            player.sendMessage("§7Items Sold: §e" + playerData.getItemsSold());
            player.sendMessage("§7Items Bought: §e" + playerData.getItemsBought());
            player.sendMessage("§7Total Earnings: §6" + plugin.getEconomyManager().formatMoney(playerData.getTotalEarnings()));
            player.sendMessage("§7Total Spent: §6" + plugin.getEconomyManager().formatMoney(playerData.getTotalSpent()));
            
            double profit = playerData.getTotalEarnings() - playerData.getTotalSpent();
            String profitColor = profit >= 0 ? "§a" : "§c";
            player.sendMessage("§7Net Profit: " + profitColor + plugin.getEconomyManager().formatMoney(profit));
            
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error getting transaction stats for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle viewing another player's transactions
     */
    private boolean handlePlayerTransactions(Player player, String targetPlayerName) {
        // Try to get the target player (online or offline)
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        
        // Check if the player has ever joined the server
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found", "player", targetPlayerName));
            return true;
        }
        
        // Open transaction history GUI for the target player
        try {
            TransactionHistoryGUI gui = new TransactionHistoryGUI(plugin, player, targetPlayer);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            gui.open();
            
            // Send message to admin about whose transactions they're viewing
            player.sendMessage(plugin.getConfigManager().getMessage("admin.viewing-transactions", "player", targetPlayer.getName()));
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error opening transaction history GUI for " + targetPlayerName + " (viewed by " + player.getName() + "): " + e.getMessage());
        }
        
        return true;
    }

    /**
     * Handle help subcommand
     */
    private boolean handleHelp(Player player) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        
        player.sendMessage(prefix + "§9Transaction History Help:");
        player.sendMessage("§7/transactions §8- §eView transaction history");
        player.sendMessage("§7/transactions stats §8- §eView your statistics");
        
        if (player.hasPermission("marketplace.history.other")) {
            player.sendMessage("§7/transactions <player> §8- §eView another player's transactions");
        }
        
        if (player.hasPermission("marketplace.admin.clear")) {
            player.sendMessage("§7/transactions clear §8- §eClear transaction history");
        }
        
        player.sendMessage("");
        player.sendMessage("§7Transaction Types:");
        player.sendMessage("§7• §aGreen §7- Items you purchased");
        player.sendMessage("§7• §eYellow §7- Items you sold");
        player.sendMessage("§7• §cRed §7- Black market transactions");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("stats");
            completions.add("help");
            
            if (sender.hasPermission("marketplace.admin.clear")) {
                completions.add("clear");
            }
        }

        return completions;
    }
}
