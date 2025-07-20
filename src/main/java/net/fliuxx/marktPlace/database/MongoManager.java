package net.fliuxx.marktPlace.database;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.fliuxx.marktPlace.MarktPlace;
import net.fliuxx.marktPlace.database.models.MarketItem;
import net.fliuxx.marktPlace.database.models.PlayerData;
import net.fliuxx.marktPlace.database.models.Transaction;
import net.fliuxx.marktPlace.database.models.TimerState;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB Manager for MarketPlace Plugin
 * Handles all database operations
 */
public class MongoManager {

    private final MarktPlace plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    // Collections
    private MongoCollection<Document> playersCollection;
    private MongoCollection<Document> marketItemsCollection;
    private MongoCollection<Document> transactionsCollection;
    private MongoCollection<Document> blackMarketCollection;
    private MongoCollection<Document> timerStateCollection;

    public MongoManager(MarktPlace plugin) {
        this.plugin = plugin;
    }

    /**
     * Connect to MongoDB database
     */
    public boolean connect() {
        try {
            FileConfiguration config = plugin.getConfig();
            
            String connectionString = config.getString("database.mongodb.connection-string");
            
            if (connectionString != null && !connectionString.isEmpty()) {
                // Use connection string
                mongoClient = MongoClients.create(connectionString);
            } else {
                // Use individual settings
                String host = config.getString("database.mongodb.host", "localhost");
                int port = config.getInt("database.mongodb.port", 27017);
                String username = config.getString("database.mongodb.username", "");
                String password = config.getString("database.mongodb.password", "");
                
                StringBuilder connectionBuilder = new StringBuilder("mongodb://");
                
                if (!username.isEmpty() && !password.isEmpty()) {
                    connectionBuilder.append(username).append(":").append(password).append("@");
                }
                
                connectionBuilder.append(host).append(":").append(port);
                
                mongoClient = MongoClients.create(connectionBuilder.toString());
            }
            
            String databaseName = config.getString("database.mongodb.database", "marketplace");
            database = mongoClient.getDatabase(databaseName);
            
            // Initialize collections
            playersCollection = database.getCollection("players");
            marketItemsCollection = database.getCollection("market_items");
            transactionsCollection = database.getCollection("transactions");
            blackMarketCollection = database.getCollection("black_market");
            timerStateCollection = database.getCollection("timer_state");
            
            // Test connection
            database.runCommand(new Document("ping", 1));
            
            plugin.getLogger().info("Successfully connected to MongoDB!");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MongoDB: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disconnect from MongoDB
     */
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            // Removed excessive logging - only log on startup/shutdown
        }
    }

    /**
     * Get or create player data
     */
    public PlayerData getPlayerData(UUID playerId) {
        Document doc = playersCollection.find(Filters.eq("_id", playerId.toString())).first();
        
        if (doc != null) {
            return PlayerData.fromDocument(doc);
        } else {
            // Create new player data
            PlayerData playerData = new PlayerData(playerId);
            savePlayerData(playerData);
            return playerData;
        }
    }

    /**
     * Save player data
     */
    public void savePlayerData(PlayerData playerData) {
        Document doc = playerData.toDocument();
        playersCollection.replaceOne(
            Filters.eq("_id", playerData.getPlayerId().toString()),
            doc,
            new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    /**
     * Add item to marketplace
     */
    public void addMarketItem(MarketItem item) {
        Document doc = item.toDocument();
        marketItemsCollection.insertOne(doc);
    }

    /**
     * Remove item from marketplace
     */
    public void removeMarketItem(String itemId) {
        marketItemsCollection.deleteOne(Filters.eq("_id", itemId));
    }

    /**
     * Get all market items
     */
    public List<MarketItem> getAllMarketItems() {
        List<MarketItem> items = new ArrayList<>();
        
        for (Document doc : marketItemsCollection.find()) {
            items.add(MarketItem.fromDocument(doc));
        }
        
        return items;
    }

    /**
     * Get market items by seller
     */
    public List<MarketItem> getMarketItemsBySeller(UUID sellerId) {
        List<MarketItem> items = new ArrayList<>();
        
        for (Document doc : marketItemsCollection.find(Filters.eq("sellerId", sellerId.toString()))) {
            items.add(MarketItem.fromDocument(doc));
        }
        
        return items;
    }

    /**
     * Get market item by ID
     */
    public MarketItem getMarketItem(String itemId) {
        Document doc = marketItemsCollection.find(Filters.eq("_id", itemId)).first();
        return doc != null ? MarketItem.fromDocument(doc) : null;
    }

    /**
     * Update market item
     */
    public void updateMarketItem(MarketItem item) {
        Document doc = item.toDocument();
        marketItemsCollection.replaceOne(
            Filters.eq("_id", item.getId()),
            doc
        );
    }

    /**
     * Add transaction
     */
    public void addTransaction(Transaction transaction) {
        Document doc = transaction.toDocument();
        transactionsCollection.insertOne(doc);
    }

    /**
     * Get transactions by player
     */
    public List<Transaction> getTransactionsByPlayer(UUID playerId) {
        List<Transaction> transactions = new ArrayList<>();
        
        Bson filter = Filters.or(
            Filters.eq("buyerId", playerId.toString()),
            Filters.eq("sellerId", playerId.toString())
        );
        
        for (Document doc : transactionsCollection.find(filter).sort(new Document("timestamp", -1))) {
            transactions.add(Transaction.fromDocument(doc));
        }
        
        return transactions;
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        for (Document doc : transactionsCollection.find().sort(new Document("timestamp", -1))) {
            transactions.add(Transaction.fromDocument(doc));
        }
        
        return transactions;
    }

    /**
     * Add item to black market
     */
    public void addBlackMarketItem(MarketItem item) {
        Document doc = item.toDocument();
        blackMarketCollection.insertOne(doc);
    }

    /**
     * Remove item from black market
     */
    public void removeBlackMarketItem(String itemId) {
        blackMarketCollection.deleteOne(Filters.eq("_id", itemId));
    }

    /**
     * Get all black market items
     */
    public List<MarketItem> getAllBlackMarketItems() {
        List<MarketItem> items = new ArrayList<>();
        
        for (Document doc : blackMarketCollection.find()) {
            items.add(MarketItem.fromDocument(doc));
        }
        
        return items;
    }

    /**
     * Get black market items (alias for getAllBlackMarketItems)
     */
    public List<MarketItem> getBlackMarketItems() {
        return getAllBlackMarketItems();
    }

    /**
     * Move item to black market (removes from market and adds to black market)
     */
    public void moveItemToBlackMarket(MarketItem item) {
        // Remove from regular market
        removeMarketItem(item.getId());
        
        // Add to black market
        addBlackMarketItem(item);
    }

    /**
     * Clear all black market items
     */
    public void clearBlackMarket() {
        blackMarketCollection.deleteMany(new Document());
    }

    /**
     * Get black market item by ID
     */
    public MarketItem getBlackMarketItem(String itemId) {
        Document doc = blackMarketCollection.find(Filters.eq("_id", itemId)).first();
        return doc != null ? MarketItem.fromDocument(doc) : null;
    }

    /**
     * Remove expired listings
     */
    public void removeExpiredListings() {
        long currentTime = System.currentTimeMillis();
        long listingDuration = plugin.getConfig().getLong("general.listing-duration", 604800) * 1000;
        long expirationTime = currentTime - listingDuration;
        
        marketItemsCollection.deleteMany(Filters.lt("listedAt", expirationTime));
    }

    /**
     * Get count of active listings by player
     */
    public long getActiveListingsCount(UUID playerId) {
        return marketItemsCollection.countDocuments(Filters.eq("sellerId", playerId.toString()));
    }

    /**
     * Get all market items by player
     */
    public List<MarketItem> getPlayerMarketItems(UUID playerId) {
        List<MarketItem> items = new ArrayList<>();
        try {
            FindIterable<Document> docs = marketItemsCollection.find(Filters.eq("sellerId", playerId.toString()));
            for (Document doc : docs) {
                items.add(MarketItem.fromDocument(doc));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player market items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Get all black market items by player
     */
    public List<MarketItem> getPlayerBlackMarketItems(UUID playerId) {
        List<MarketItem> items = new ArrayList<>();
        try {
            FindIterable<Document> docs = blackMarketCollection.find(Filters.eq("sellerId", playerId.toString()));
            for (Document doc : docs) {
                items.add(MarketItem.fromDocument(doc));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player black market items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Move items from black market back to regular market
     */
    public void moveBlackMarketItemsToMarket() {
        try {
            // Get all black market items
            FindIterable<Document> docs = blackMarketCollection.find();
            
            for (Document doc : docs) {
                // Convert back to regular market item
                MarketItem item = MarketItem.fromDocument(doc);
                item.setBlackMarket(false);
                item.setPrice(item.getOriginalPrice()); // Reset to original price
                
                // Add to regular market
                marketItemsCollection.insertOne(item.toDocument());
            }
            
            // Clear black market
            blackMarketCollection.deleteMany(new Document());
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error moving black market items to market: " + e.getMessage());
        }
    }

    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        try {
            if (mongoClient != null && database != null) {
                database.runCommand(new Document("ping", 1));
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Database connection check failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Save timer state to database
     */
    public void saveTimerState(TimerState timerState) {
        try {
            Document doc = timerState.toDocument();
            timerStateCollection.replaceOne(
                Filters.eq("_id", timerState.getId()),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );
            // Removed frequent timer state logging to reduce console spam
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving timer state: " + e.getMessage());
        }
    }

    /**
     * Load timer state from database
     */
    public TimerState loadTimerState(String timerId) {
        try {
            Document doc = timerStateCollection.find(Filters.eq("_id", timerId)).first();
            if (doc != null) {
                TimerState state = TimerState.fromDocument(doc);
                // Only log on first load, not every time
                return state;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading timer state: " + e.getMessage());
        }
        
        // Return new timer state if not found
        // Only log the first time a new timer state is created
        return new TimerState();
    }
}
