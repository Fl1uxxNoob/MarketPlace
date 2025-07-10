package net.fliuxx.marktPlace.database.models;

import org.bson.Document;

import java.util.UUID;

/**
 * Transaction Model
 * Represents a completed transaction
 */
public class Transaction {

    private String id;
    private UUID buyerId;
    private String buyerName;
    private UUID sellerId;
    private String sellerName;
    private String itemName;
    private String itemData; // Serialized item data
    private double price;
    private long timestamp;
    private TransactionType type;

    public enum TransactionType {
        NORMAL,
        BLACK_MARKET
    }

    public Transaction(String id, UUID buyerId, String buyerName, UUID sellerId, String sellerName,
                      String itemName, String itemData, double price, TransactionType type) {
        this.id = id;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.itemData = itemData;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public Transaction(String id, UUID buyerId, String buyerName, UUID sellerId, String sellerName,
                      String itemName, String itemData, double price, long timestamp, TransactionType type) {
        this.id = id;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.itemData = itemData;
        this.price = price;
        this.timestamp = timestamp;
        this.type = type;
    }

    /**
     * Convert to MongoDB Document
     */
    public Document toDocument() {
        Document doc = new Document();
        doc.append("_id", id);
        doc.append("buyerId", buyerId.toString());
        doc.append("buyerName", buyerName);
        doc.append("sellerId", sellerId.toString());
        doc.append("sellerName", sellerName);
        doc.append("itemName", itemName);
        doc.append("itemData", itemData);
        doc.append("price", price);
        doc.append("timestamp", timestamp);
        doc.append("type", type.name());
        return doc;
    }

    /**
     * Create from MongoDB Document
     */
    public static Transaction fromDocument(Document doc) {
        String id = doc.getString("_id");
        UUID buyerId = UUID.fromString(doc.getString("buyerId"));
        String buyerName = doc.getString("buyerName");
        UUID sellerId = UUID.fromString(doc.getString("sellerId"));
        String sellerName = doc.getString("sellerName");
        String itemName = doc.getString("itemName");
        String itemData = doc.getString("itemData");
        double price = doc.getDouble("price");
        long timestamp = doc.getLong("timestamp");
        TransactionType type = TransactionType.valueOf(doc.getString("type") != null ? doc.getString("type") : "NORMAL");
        
        return new Transaction(id, buyerId, buyerName, sellerId, sellerName, itemName, itemData, 
                             price, timestamp, type);
    }

    /**
     * Get formatted timestamp
     */
    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    /**
     * Get time since transaction
     */
    public String getTimeSince() {
        long timeDiff = System.currentTimeMillis() - timestamp;
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return seconds + "s ago";
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
