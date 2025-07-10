package net.fliuxx.marktPlace.database.models;

import org.bson.Document;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Market Item Model
 * Represents an item listed in the marketplace
 */
public class MarketItem {

    private String id;
    private UUID sellerId;
    private String sellerName;
    private ItemStack itemStack;
    private String itemData; // Serialized item data
    private double price;
    private long listedAt;
    private boolean isBlackMarket;
    private double originalPrice; // For black market items

    public MarketItem(String id, UUID sellerId, String sellerName, ItemStack itemStack, 
                     String itemData, double price) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemData = itemData;
        this.price = price;
        this.listedAt = System.currentTimeMillis();
        this.isBlackMarket = false;
        this.originalPrice = price;
    }

    public MarketItem(String id, UUID sellerId, String sellerName, ItemStack itemStack, 
                     String itemData, double price, long listedAt, boolean isBlackMarket, 
                     double originalPrice) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemData = itemData;
        this.price = price;
        this.listedAt = listedAt;
        this.isBlackMarket = isBlackMarket;
        this.originalPrice = originalPrice;
    }

    /**
     * Convert to MongoDB Document
     */
    public Document toDocument() {
        Document doc = new Document();
        doc.append("_id", id);
        doc.append("sellerId", sellerId.toString());
        doc.append("sellerName", sellerName);
        doc.append("itemData", itemData);
        doc.append("price", price);
        doc.append("listedAt", listedAt);
        doc.append("isBlackMarket", isBlackMarket);
        doc.append("originalPrice", originalPrice);
        return doc;
    }

    /**
     * Create from MongoDB Document
     */
    public static MarketItem fromDocument(Document doc) {
        String id = doc.getString("_id");
        UUID sellerId = UUID.fromString(doc.getString("sellerId"));
        String sellerName = doc.getString("sellerName");
        String itemData = doc.getString("itemData");
        double price = doc.getDouble("price");
        long listedAt = doc.getLong("listedAt");
        boolean isBlackMarket = doc.getBoolean("isBlackMarket", false);
        double originalPrice = doc.getDouble("originalPrice") != null ? doc.getDouble("originalPrice") : price;
        
        return new MarketItem(id, sellerId, sellerName, null, itemData, price, 
                            listedAt, isBlackMarket, originalPrice);
    }

    /**
     * Check if the listing has expired
     */
    public boolean isExpired(long listingDuration) {
        return System.currentTimeMillis() - listedAt > listingDuration;
    }

    /**
     * Get formatted time since listing
     */
    public String getTimeSinceListing() {
        long timeDiff = System.currentTimeMillis() - listedAt;
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getListedAt() {
        return listedAt;
    }

    public void setListedAt(long listedAt) {
        this.listedAt = listedAt;
    }

    public boolean isBlackMarket() {
        return isBlackMarket;
    }

    public void setBlackMarket(boolean blackMarket) {
        isBlackMarket = blackMarket;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }
}
