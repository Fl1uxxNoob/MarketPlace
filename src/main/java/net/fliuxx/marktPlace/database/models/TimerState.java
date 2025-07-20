package net.fliuxx.marktPlace.database.models;

import org.bson.Document;

/**
 * Simple Timer State Model for BlackMarket refresh persistence
 * Stores exact timer state for perfect restoration
 */
public class TimerState {
    private String id;
    private long nextRefreshTime;
    private long lastRefreshTime;
    private long saveTime;

    public TimerState() {
        this.id = "blackmarket_timer";
        this.nextRefreshTime = 0;
        this.lastRefreshTime = System.currentTimeMillis();
        this.saveTime = System.currentTimeMillis();
    }

    public TimerState(String id, long nextRefreshTime, long lastRefreshTime) {
        this.id = id;
        this.nextRefreshTime = nextRefreshTime;
        this.lastRefreshTime = lastRefreshTime;
        this.saveTime = System.currentTimeMillis();
    }

    // Convert to MongoDB Document
    public Document toDocument() {
        Document doc = new Document();
        doc.append("_id", id);
        doc.append("nextRefreshTime", nextRefreshTime);
        doc.append("lastRefreshTime", lastRefreshTime);
        doc.append("saveTime", saveTime);
        return doc;
    }

    // Create from MongoDB Document
    public static TimerState fromDocument(Document doc) {
        TimerState state = new TimerState();
        state.id = doc.getString("_id");
        state.nextRefreshTime = doc.getLong("nextRefreshTime") != null ? doc.getLong("nextRefreshTime") : 0;
        state.lastRefreshTime = doc.getLong("lastRefreshTime") != null ? doc.getLong("lastRefreshTime") : System.currentTimeMillis();
        state.saveTime = doc.getLong("saveTime") != null ? doc.getLong("saveTime") : System.currentTimeMillis();
        return state;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getNextRefreshTime() {
        return nextRefreshTime;
    }

    public void setNextRefreshTime(long nextRefreshTime) {
        this.nextRefreshTime = nextRefreshTime;
        this.saveTime = System.currentTimeMillis();
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(long lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
        this.saveTime = System.currentTimeMillis();
    }

    public long getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(long saveTime) {
        this.saveTime = saveTime;
    }

    /**
     * Get remaining time until next refresh
     */
    public long getRemainingTime() {
        if (nextRefreshTime <= 0) {
            return -1;
        }
        return Math.max(0, nextRefreshTime - System.currentTimeMillis());
    }

    /**
     * Check if timer has expired
     */
    public boolean hasExpired() {
        return nextRefreshTime > 0 && System.currentTimeMillis() >= nextRefreshTime;
    }

    @Override
    public String toString() {
        return "TimerState{" +
                "id='" + id + '\'' +
                ", nextRefreshTime=" + nextRefreshTime +
                ", lastRefreshTime=" + lastRefreshTime +
                ", saveTime=" + saveTime +
                ", remainingTime=" + getRemainingTime() +
                '}';
    }
}