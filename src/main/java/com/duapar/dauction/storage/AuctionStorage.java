package com.duapar.dauction.storage;

import com.duapar.dauction.model.AuctionListing;

import java.util.Map;

public interface AuctionStorage {

    void init() throws Exception;

    /**
     * Charge toutes les annonces connues. Clé de la map = identifiant de l'annonce.
     */
    Map<Integer, AuctionListing> loadListings() throws Exception;

    void saveListing(AuctionListing listing);

    void close();
}
