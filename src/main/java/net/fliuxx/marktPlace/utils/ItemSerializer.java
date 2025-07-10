package net.fliuxx.marktPlace.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Item Serializer Utility
 * Handles serialization and deserialization of ItemStacks
 */
public class ItemSerializer {

    /**
     * Serialize ItemStack to Base64 string
     */
    public static String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeObject(item);
            dataOutput.close();
            
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing ItemStack", e);
        }
    }

    /**
     * Deserialize Base64 string to ItemStack
     */
    public static ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            
            return item;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error deserializing ItemStack", e);
        }
    }

    /**
     * Serialize ItemStack array to Base64 string
     */
    public static String serializeItemStackArray(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing ItemStack array", e);
        }
    }

    /**
     * Deserialize Base64 string to ItemStack array
     */
    public static ItemStack[] deserializeItemStackArray(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            
            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error deserializing ItemStack array", e);
        }
    }

    /**
     * Get display name of ItemStack
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null) return "Unknown Item";
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        // Format material name
        String materialName = item.getType().name();
        materialName = materialName.replace("_", " ");
        materialName = capitalizeWords(materialName);
        
        return materialName;
    }

    /**
     * Capitalize each word in a string
     */
    private static String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Check if ItemStack is valid (not null and not air)
     */
    public static boolean isValidItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }

    /**
     * Clone ItemStack safely
     */
    public static ItemStack cloneItem(ItemStack item) {
        if (item == null) return null;
        return item.clone();
    }

    /**
     * Compare two ItemStacks for equality (including NBT)
     */
    public static boolean areItemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;
        
        return item1.isSimilar(item2) && item1.getAmount() == item2.getAmount();
    }
}
