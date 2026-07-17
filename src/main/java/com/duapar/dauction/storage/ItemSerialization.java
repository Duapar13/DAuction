package com.duapar.dauction.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * (Dé)sérialise un ItemStack en une chaîne Base64, pour le stocker aussi bien dans
 * un fichier YAML que dans une colonne texte MySQL avec le même code des deux côtés.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static String serialize(ItemStack item) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(byteStream)) {
            objectStream.writeObject(item);
            objectStream.flush();
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de sérialiser l'ItemStack", e);
        }
    }

    public static ItemStack deserialize(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream objectStream = new BukkitObjectInputStream(byteStream)) {
            return (ItemStack) objectStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Impossible de désérialiser l'ItemStack", e);
        }
    }
}
