package com.duapar.dauction.manager;

import com.duapar.dapi.service.EconomyService;
import com.duapar.dauction.model.AuctionListing;
import com.duapar.dauction.model.AuctionStatus;
import com.duapar.dauction.storage.AuctionStorage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionManager {

    private final AuctionStorage storage;
    private final EconomyService economyService;

    private final Map<Integer, AuctionListing> listings = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private long durationMillis;
    private int maxActivePerPlayer;
    private long minPrice;
    private long maxPrice;
    private int taxPercent;
    private final Set<Material> blacklist = EnumSet.noneOf(Material.class);

    public AuctionManager(AuctionStorage storage, EconomyService economyService) {
        this.storage = storage;
        this.economyService = economyService;
    }

    public void loadConfig(JavaPlugin plugin, Logger logger) {
        FileConfiguration cfg = plugin.getConfig();
        this.durationMillis = Math.max(1, cfg.getInt("listing.duration-hours", 48)) * 3_600_000L;
        this.maxActivePerPlayer = cfg.getInt("listing.max-active-per-player", 5);
        this.minPrice = cfg.getLong("listing.min-price", 1);
        this.maxPrice = cfg.getLong("listing.max-price", 1_000_000);
        this.taxPercent = cfg.getInt("listing.tax-percent", 5);

        blacklist.clear();
        for (String name : cfg.getStringList("blacklisted-items")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                blacklist.add(material);
            } else {
                logger.log(Level.WARNING, "Objet blacklisté inconnu dans config.yml: " + name);
            }
        }
    }

    public void seed(Map<Integer, AuctionListing> loaded) {
        listings.clear();
        listings.putAll(loaded);
        int maxId = 0;
        for (int id : loaded.keySet()) {
            maxId = Math.max(maxId, id);
        }
        nextId.set(maxId + 1);
    }

    public String format(long amount) {
        return economyService.format(amount);
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    // ---------------------------------------------------------------- création

    public AuctionListing createListing(UUID sellerId, String sellerName, ItemStack item, long price) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            throw new AuctionException("Tiens l'objet à vendre en main.");
        }
        if (blacklist.contains(item.getType())) {
            throw new AuctionException("Cet objet ne peut pas être mis en vente.");
        }
        if (price < minPrice || price > maxPrice) {
            throw new AuctionException("Le prix doit être entre " + format(minPrice) + " et " + format(maxPrice) + ".");
        }
        if (countActive(sellerId) >= maxActivePerPlayer) {
            throw new AuctionException("Tu as déjà " + maxActivePerPlayer + " annonces actives au maximum. "
                    + "Annule ou attends qu'une se vende/expire.");
        }

        int id = nextId.getAndIncrement();
        long now = System.currentTimeMillis();
        AuctionListing listing = new AuctionListing(id, sellerId, sellerName, item, price,
                AuctionStatus.ACTIVE, now, now + durationMillis, null, null, null);
        listings.put(id, listing);
        storage.saveListing(listing);
        return listing;
    }

    // ---------------------------------------------------------------- achat / annulation / retrait

    public AuctionListing buy(Player buyer, int id) {
        AuctionListing listing = getActiveOrThrow(id);
        if (listing.getSellerId().equals(buyer.getUniqueId())) {
            throw new AuctionException("Tu ne peux pas acheter ta propre annonce.");
        }
        if (!economyService.has(buyer.getUniqueId(), listing.getPrice())) {
            throw new AuctionException("Solde insuffisant (" + format(listing.getPrice()) + " requis).");
        }

        economyService.withdraw(buyer.getUniqueId(), listing.getPrice());
        long tax = listing.getPrice() * taxPercent / 100;
        long sellerReceives = listing.getPrice() - tax;
        economyService.deposit(listing.getSellerId(), sellerReceives);

        listing.setStatus(AuctionStatus.SOLD);
        listing.setBuyer(buyer.getUniqueId(), buyer.getName());
        listing.setResolvedAt(System.currentTimeMillis());
        storage.saveListing(listing);
        return listing;
    }

    public AuctionListing cancel(UUID cancellerId, boolean isAdmin, int id) {
        AuctionListing listing = getActiveOrThrow(id);
        if (!isAdmin && !listing.getSellerId().equals(cancellerId)) {
            throw new AuctionException("Ce n'est pas ta mise en vente.");
        }
        listing.setStatus(AuctionStatus.CANCELLED);
        listing.setResolvedAt(System.currentTimeMillis());
        storage.saveListing(listing);
        return listing;
    }

    /**
     * @return l'annonce (retirée de son état "non réclamée") dont l'objet doit être
     * donné au joueur par l'appelant.
     */
    public AuctionListing collect(UUID playerId, int id) {
        AuctionListing listing = listings.get(id);
        if (listing == null) {
            throw new AuctionException("Annonce introuvable: #" + id);
        }
        if (!listing.getSellerId().equals(playerId)) {
            throw new AuctionException("Ce n'est pas ta mise en vente.");
        }
        if (!listing.isUnclaimed()) {
            throw new AuctionException("Il n'y a rien à récupérer pour cette annonce.");
        }
        listing.setStatus(AuctionStatus.COLLECTED);
        storage.saveListing(listing);
        return listing;
    }

    public void expireOldListings(java.util.function.Consumer<AuctionListing> onExpired) {
        long now = System.currentTimeMillis();
        for (AuctionListing listing : listings.values()) {
            if (listing.getStatus() == AuctionStatus.ACTIVE && listing.getExpiresAt() <= now) {
                listing.setStatus(AuctionStatus.EXPIRED);
                listing.setResolvedAt(now);
                storage.saveListing(listing);
                onExpired.accept(listing);
            }
        }
    }

    // ---------------------------------------------------------------- consultation

    public AuctionListing getActiveOrThrow(int id) {
        AuctionListing listing = listings.get(id);
        if (listing == null || listing.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionException("Annonce introuvable ou déjà résolue: #" + id);
        }
        return listing;
    }

    public int countActive(UUID sellerId) {
        int count = 0;
        for (AuctionListing listing : listings.values()) {
            if (listing.getStatus() == AuctionStatus.ACTIVE && listing.getSellerId().equals(sellerId)) {
                count++;
            }
        }
        return count;
    }

    public int countActiveAll() {
        int count = 0;
        for (AuctionListing listing : listings.values()) {
            if (listing.getStatus() == AuctionStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    public List<AuctionListing> getActive() {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (listing.getStatus() == AuctionStatus.ACTIVE) {
                result.add(listing);
            }
        }
        result.sort(Comparator.comparingLong(AuctionListing::getListedAt).reversed());
        return result;
    }

    public List<AuctionListing> getOwnActive(UUID sellerId) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (listing.getStatus() == AuctionStatus.ACTIVE && listing.getSellerId().equals(sellerId)) {
                result.add(listing);
            }
        }
        result.sort(Comparator.comparingLong(AuctionListing::getListedAt).reversed());
        return result;
    }

    public List<AuctionListing> getOwnUnclaimed(UUID sellerId) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (listing.isUnclaimed() && listing.getSellerId().equals(sellerId)) {
                result.add(listing);
            }
        }
        result.sort(Comparator.comparingLong(AuctionListing::getListedAt).reversed());
        return result;
    }

    public List<AuctionListing> getOwnHistory(UUID playerId, int limit) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            boolean involved = listing.getSellerId().equals(playerId) || playerId.equals(listing.getBuyerId());
            if (listing.getStatus() != AuctionStatus.ACTIVE && involved) {
                result.add(listing);
            }
        }
        result.sort(Comparator.comparingLong(l -> l.getResolvedAt() != null ? -l.getResolvedAt() : 0));
        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }
}
