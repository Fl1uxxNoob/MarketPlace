package net.fliuxx.marktPlace.database.models;

import org.bson.Document;

import java.util.UUID;

/**
 * Player Data Model
 * Stores player-specific information
 */
public class PlayerData {

    private UUID playerId;
    private String playerName;
    private long totalEarnings;
    private long totalSpent;
    private int itemsSold;
    private int itemsBought;
    private long lastActive;
    private long firstJoined;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.playerName = "";
        this.totalEarnings = 0;
        this.totalSpent = 0;
        this.itemsSold = 0;
        this.itemsBought = 0;
        this.lastActive = System.currentTimeMillis();
        this.firstJoined = System.currentTimeMillis();
    }

    public PlayerData(UUID playerId, String playerName, long totalEarnings, long totalSpent, 
                     int itemsSold, int itemsBought, long lastActive, long firstJoined) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalEarnings = totalEarnings;
        this.totalSpent = totalSpent;
        this.itemsSold = itemsSold;
        this.itemsBought = itemsBought;
        this.lastActive = lastActive;
        this.firstJoined = firstJoined;
    }

    /**
     * Convert to MongoDB Document
     */
    public Document toDocument() {
        Document doc = new Document();
        doc.append("_id", playerId.toString());
        doc.append("playerName", playerName);
        doc.append("totalEarnings", totalEarnings);
        doc.append("totalSpent", totalSpent);
        doc.append("itemsSold", itemsSold);
        doc.append("itemsBought", itemsBought);
        doc.append("lastActive", lastActive);
        doc.append("firstJoined", firstJoined);
        return doc;
    }

    /**
     * Create from MongoDB Document
     */
    public static PlayerData fromDocument(Document doc) {
        UUID playerId = UUID.fromString(doc.getString("_id"));
        String playerName = doc.getString("playerName");
        long totalEarnings = doc.getLong("totalEarnings");
        long totalSpent = doc.getLong("totalSpent");
        int itemsSold = doc.getInteger("itemsSold");
        int itemsBought = doc.getInteger("itemsBought");
        long lastActive = doc.getLong("lastActive");
        long firstJoined = doc.getLong("firstJoined");
        
        return new PlayerData(playerId, playerName, totalEarnings, totalSpent, 
                            itemsSold, itemsBought, lastActive, firstJoined);
    }

    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(long totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public void addEarnings(long amount) {
        this.totalEarnings += amount;
    }

    public long getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(long totalSpent) {
        this.totalSpent = totalSpent;
    }

    public void addSpent(long amount) {
        this.totalSpent += amount;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(int itemsSold) {
        this.itemsSold = itemsSold;
    }

    public void incrementItemsSold() {
        this.itemsSold++;
    }

    public int getItemsBought() {
        return itemsBought;
    }

    public void setItemsBought(int itemsBought) {
        this.itemsBought = itemsBought;
    }

    public void incrementItemsBought() {
        this.itemsBought++;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public void updateLastActive() {
        this.lastActive = System.currentTimeMillis();
    }

    public long getFirstJoined() {
        return firstJoined;
    }

    public void setFirstJoined(long firstJoined) {
        this.firstJoined = firstJoined;
    }
}
