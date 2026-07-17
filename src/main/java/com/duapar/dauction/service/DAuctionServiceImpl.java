package com.duapar.dauction.service;

import com.duapar.dapi.service.AuctionService;
import com.duapar.dauction.manager.AuctionManager;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Implémentation de AuctionService (contrat DAPI) adossée à AuctionManager, pour
 * qu'un autre plugin D(nom) (ex: un événement automatique) puisse mettre un objet
 * en vente sans dépendre de DAuction.
 */
public class DAuctionServiceImpl implements AuctionService {

    private final AuctionManager auctionManager;

    public DAuctionServiceImpl(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public int createListing(UUID sellerId, String sellerName, ItemStack item, long price) {
        return auctionManager.createListing(sellerId, sellerName, item, price).getId();
    }

    @Override
    public int getActiveListingCount() {
        return auctionManager.countActiveAll();
    }
}
