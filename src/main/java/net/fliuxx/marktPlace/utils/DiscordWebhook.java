package net.fliuxx.marktPlace.utils;

import net.fliuxx.marktPlace.MarktPlace;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Discord Webhook Integration
 * Handles sending transaction notifications to Discord
 */
public class DiscordWebhook {

    private final MarktPlace plugin;
    private final HttpClient httpClient;
    private String webhookUrl;
    private boolean enabled;
    private boolean embedsEnabled;
    private int embedColor;
    private String embedTitle;
    private String embedFooter;
    private String embedThumbnail;

    public DiscordWebhook(MarktPlace plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        loadConfig();
    }

    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        this.enabled = config.getBoolean("discord.enabled", true);
        this.webhookUrl = config.getString("discord.webhook-url", "");
        this.embedsEnabled = config.getBoolean("discord.embeds.enabled", true);
        this.embedColor = config.getInt("discord.embeds.color", 3447003);
        this.embedTitle = config.getString("discord.embeds.title", "MarketPlace Transaction");
        this.embedFooter = config.getString("discord.embeds.footer", "MarketPlace Plugin");
        this.embedThumbnail = config.getString("discord.embeds.thumbnail", "");
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Send a purchase notification
     */
    public void sendPurchaseNotification(String buyerName, String sellerName, String itemName, double price) {
        if (!enabled || webhookUrl.isEmpty()) return;

        String message = plugin.getConfigManager().getMessage("discord.messages.purchase",
                "player", buyerName,
                "seller", sellerName,
                "item", itemName,
                "price", String.format("%.2f", price));

        sendWebhook(message, "Purchase", Color.GREEN);
    }

    /**
     * Send a black market purchase notification
     */
    public void sendBlackMarketPurchaseNotification(String buyerName, String sellerName, String itemName, double price) {
        if (!enabled || webhookUrl.isEmpty()) return;

        String message = plugin.getConfigManager().getMessage("discord.messages.blackmarket-purchase",
                "player", buyerName,
                "seller", sellerName,
                "item", itemName,
                "price", String.format("%.2f", price));

        sendWebhook(message, "Black Market Purchase", Color.RED);
    }

    /**
     * Send a custom message
     */
    public void sendCustomMessage(String message, String title, Color color) {
        if (!enabled || webhookUrl.isEmpty()) return;
        sendWebhook(message, title, color);
    }

    /**
     * Send webhook message
     */
    private void sendWebhook(String message, String title, Color color) {
        CompletableFuture.runAsync(() -> {
            try {
                JSONObject payload = new JSONObject();

                if (embedsEnabled) {
                    // Create embed
                    JSONObject embed = new JSONObject();
                    embed.put("title", title.isEmpty() ? embedTitle : title);
                    embed.put("description", message);
                    embed.put("color", color.getRGB() & 0xFFFFFF); // Remove alpha channel
                    embed.put("timestamp", Instant.now().toString());

                    // Add footer
                    if (!embedFooter.isEmpty()) {
                        JSONObject footer = new JSONObject();
                        footer.put("text", embedFooter);
                        embed.put("footer", footer);
                    }

                    // Add thumbnail
                    if (!embedThumbnail.isEmpty()) {
                        JSONObject thumbnail = new JSONObject();
                        thumbnail.put("url", embedThumbnail);
                        embed.put("thumbnail", thumbnail);
                    }

                    payload.put("embeds", new JSONObject[]{embed});
                } else {
                    // Simple text message
                    payload.put("content", message);
                }

                // Send HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + response.statusCode() + " - " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                plugin.getLogger().warning("Error sending Discord webhook: " + e.getMessage());
            }
        });
    }

    /**
     * Test webhook connection
     */
    public boolean testWebhook() {
        if (!enabled || webhookUrl.isEmpty()) return false;

        try {
            JSONObject payload = new JSONObject();
            payload.put("content", "MarketPlace webhook test - Connection successful!");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 204;

        } catch (IOException | InterruptedException e) {
            plugin.getLogger().warning("Error testing Discord webhook: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if webhook is enabled and configured
     */
    public boolean isEnabled() {
        return enabled && !webhookUrl.isEmpty();
    }

    /**
     * Get webhook URL (masked for security)
     */
    public String getMaskedWebhookUrl() {
        if (webhookUrl.isEmpty()) return "Not configured";
        
        int lastSlash = webhookUrl.lastIndexOf('/');
        if (lastSlash > 0) {
            return webhookUrl.substring(0, lastSlash) + "/***MASKED***";
        }
        return "***MASKED***";
    }
}
