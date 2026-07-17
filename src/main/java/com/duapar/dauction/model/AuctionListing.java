package com.duapar.dauction.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionListing {

    private final int id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack item;
    private final long price;
    private AuctionStatus status;
    private final long listedAt;
    private final long expiresAt;
    private Long resolvedAt;
    private UUID buyerId;
    private String buyerName;

    public AuctionListing(int id, UUID sellerId, String sellerName, ItemStack item, long price,
                           AuctionStatus status, long listedAt, long expiresAt, Long resolvedAt,
                           UUID buyerId, String buyerName) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.status = status;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
        this.resolvedAt = resolvedAt;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    /**
     * @return l'ItemStack original tel que déposé - à cloner avant toute décoration
     * d'affichage (lore/nom), pour ne jamais altérer ce que le vendeur recevra.
     */
    public ItemStack getItem() {
        return item;
    }

    public long getPrice() {
        return price;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public long getListedAt() {
        return listedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public Long getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Long resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyer(UUID buyerId, String buyerName) {
        this.buyerId = buyerId;
        this.buyerName = buyerName;
    }

    public boolean isUnclaimed() {
        return status == AuctionStatus.CANCELLED || status == AuctionStatus.EXPIRED;
    }
}
