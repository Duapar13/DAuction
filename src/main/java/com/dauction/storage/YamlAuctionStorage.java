package com.dauction.storage;

import com.dauction.model.AuctionListing;
import com.dauction.model.AuctionStatus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlAuctionStorage implements AuctionStorage {

    private final File file;
    private final Logger logger;
    private YamlConfiguration config;

    public YamlAuctionStorage(File dataFolder, Logger logger) {
        this.file = new File(new File(dataFolder, "data"), "listings.yml");
        this.logger = logger;
    }

    @Override
    public void init() throws IOException {
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Impossible de créer le dossier de données " + dir);
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Impossible de créer " + file);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public Map<Integer, AuctionListing> loadListings() {
        Map<Integer, AuctionListing> result = new HashMap<>();
        ConfigurationSection root = config.getConfigurationSection("listings");
        if (root == null) {
            return result;
        }
        for (String idStr : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(idStr);
            if (section == null) continue;
            try {
                int id = Integer.parseInt(idStr);
                UUID sellerId = UUID.fromString(section.getString("sellerId"));
                String sellerName = section.getString("sellerName", "Inconnu");
                ItemStack item = ItemSerialization.deserialize(section.getString("item"));
                long price = section.getLong("price");
                AuctionStatus status = AuctionStatus.valueOf(section.getString("status", "ACTIVE"));
                long listedAt = section.getLong("listedAt");
                long expiresAt = section.getLong("expiresAt");
                Long resolvedAt = section.contains("resolvedAt") ? section.getLong("resolvedAt") : null;
                String buyerIdStr = section.getString("buyerId", "");
                UUID buyerId = buyerIdStr.isEmpty() ? null : UUID.fromString(buyerIdStr);
                String buyerName = section.getString("buyerName", "");

                result.put(id, new AuctionListing(id, sellerId, sellerName, item, price, status,
                        listedAt, expiresAt, resolvedAt, buyerId, buyerName.isEmpty() ? null : buyerName));
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Annonce invalide ignorée (id=" + idStr + "): " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public synchronized void saveListing(AuctionListing listing) {
        String base = "listings." + listing.getId();
        config.set(base + ".sellerId", listing.getSellerId().toString());
        config.set(base + ".sellerName", listing.getSellerName());
        config.set(base + ".item", ItemSerialization.serialize(listing.getItem()));
        config.set(base + ".price", listing.getPrice());
        config.set(base + ".status", listing.getStatus().name());
        config.set(base + ".listedAt", listing.getListedAt());
        config.set(base + ".expiresAt", listing.getExpiresAt());
        config.set(base + ".resolvedAt", listing.getResolvedAt());
        config.set(base + ".buyerId", listing.getBuyerId() == null ? "" : listing.getBuyerId().toString());
        config.set(base + ".buyerName", listing.getBuyerName() == null ? "" : listing.getBuyerName());
        save();
    }

    @Override
    public void close() {
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossible de sauvegarder " + file, e);
        }
    }
}
