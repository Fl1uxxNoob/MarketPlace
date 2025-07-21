package net.fliuxx.marktPlace.commands;

import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.gui.MarketplaceGUI;
import net.fliuxx.marktPlace.gui.AdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * MarketPlace Command
 * Handles /marketplace command and its subcommands
 */
public class MarketPlaceCommand implements CommandExecutor, TabCompleter {

    private final MarktPlace plugin;

    public MarketPlaceCommand(MarktPlace plugin) {
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
        if (!player.hasPermission("marketplace.view")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "admin":
                    return handleAdmin(player);
                case "reload":
                    return handleReload(player);
                case "help":
                    return handleHelp(player);
                default:
                    player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                    return true;
            }
        }

        // Open marketplace GUI
        try {
            MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            gui.open();
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error opening marketplace GUI for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle admin subcommand
     */
    private boolean handleAdmin(Player player) {
        if (!player.hasPermission("marketplace.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        try {
            AdminGUI gui = new AdminGUI(plugin, player);
            plugin.getGUIManager().registerGUI(player.getUniqueId(), gui);
            gui.open();
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error opening admin GUI for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle reload subcommand
     */
    private boolean handleReload(Player player) {
        if (!player.hasPermission("marketplace.admin.reload")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        try {
            plugin.reloadPluginConfig();
            player.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.database-error"));
            plugin.getLogger().severe("Error reloading config: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle help subcommand
     */
    private boolean handleHelp(Player player) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        
        player.sendMessage(prefix + "§6MarketPlace Help:");
        player.sendMessage("§7/marketplace §8- §eOpen the marketplace");
        player.sendMessage("§7/sell <price> §8- §eList item in hand for sale");
        player.sendMessage("§7/blackmarket §8- §eOpen the black market");
        player.sendMessage("§7/transactions §8- §eView transaction history");
        
        if (player.hasPermission("marketplace.blackmarket.refresh")) {
            player.sendMessage("§7/blackmarketrefresh §8- §eRefresh black market");
        }
        
        if (player.hasPermission("marketplace.admin")) {
            player.sendMessage("§7/marketplace admin §8- §eOpen admin panel");
        }
        
        if (player.hasPermission("marketplace.admin.reload")) {
            player.sendMessage("§7/marketplace reload §8- §eReload configuration");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("marketplace.admin")) {
                completions.add("admin");
            }
            if (sender.hasPermission("marketplace.admin.reload")) {
                completions.add("reload");
            }
            completions.add("help");
        }

        return completions;
    }
}
