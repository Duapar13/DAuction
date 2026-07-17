package com.duapar.dauction.tasks;

import com.duapar.dauction.manager.AuctionManager;
import com.duapar.dauction.util.Display;
import com.duapar.dauction.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Vérifie périodiquement les annonces actives arrivées à expiration et prévient
 * le vendeur (s'il est connecté) qu'il peut récupérer son objet via /ah reclaim.
 */
public class ExpirationTask extends BukkitRunnable {

    private final AuctionManager auctionManager;

    public ExpirationTask(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public void run() {
        auctionManager.expireOldListings(listing -> {
            Player seller = Bukkit.getPlayer(listing.getSellerId());
            if (seller != null) {
                Msg.send(seller, "Ta mise en vente #" + listing.getId() + " (" + Display.itemName(listing.getItem())
                        + ") a expiré sans trouver preneur. Récupère l'objet avec /ah reclaim.");
            }
        });
    }
}
