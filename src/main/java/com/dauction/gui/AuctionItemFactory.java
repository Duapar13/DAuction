package com.dauction.gui;

import com.dauction.manager.AuctionManager;
import com.dauction.model.AuctionListing;
import com.dauction.model.AuctionStatus;
import com.dauction.util.TimeFormat;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

final class AuctionItemFactory {

    private AuctionItemFactory() {
    }

    static ItemStack buildBrowseItem(AuctionListing listing, AuctionManager manager, String factionLine, boolean own) {
        return decorate(listing, manager, factionLine, true, false, own);
    }

    static ItemStack buildMyItem(AuctionListing listing, AuctionManager manager) {
        return decorate(listing, manager, null, false, false, false);
    }

    static ItemStack buildReclaimItem(AuctionListing listing, AuctionManager manager) {
        return decorate(listing, manager, null, false, true, false);
    }

    private static ItemStack decorate(AuctionListing listing, AuctionManager manager, String factionLine,
                                       boolean browsing, boolean reclaiming, boolean own) {
        ItemStack display = listing.getItem().clone();
        ItemMeta meta = display.getItemMeta();

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
            lore.add("");
        }
        lore.add(ChatColor.DARK_GRAY + "Annonce #" + listing.getId());
        if (browsing) {
            lore.add(ChatColor.GRAY + "Vendeur: " + ChatColor.WHITE + listing.getSellerName()
                    + (own ? ChatColor.AQUA + " (toi)" : ""));
            if (factionLine != null) {
                lore.add(ChatColor.GRAY + factionLine);
            }
        }
        lore.add(ChatColor.GRAY + "Prix: " + ChatColor.GOLD + manager.format(listing.getPrice()));

        if (reclaiming) {
            String statusLabel = listing.getStatus() == AuctionStatus.EXPIRED
                    ? ChatColor.YELLOW + "Expirée" : ChatColor.RED + "Annulée";
            lore.add(ChatColor.GRAY + "Statut: " + statusLabel);
            lore.add("");
            lore.add(ChatColor.GREEN + "Clique pour récupérer.");
        } else {
            long remaining = listing.getExpiresAt() - System.currentTimeMillis();
            lore.add(ChatColor.GRAY + "Expire dans: " + ChatColor.WHITE + TimeFormat.remaining(remaining));
            lore.add("");
            if (browsing) {
                lore.add(own ? ChatColor.RED + "Clique pour annuler ta mise en vente." : ChatColor.GREEN + "Clique pour acheter.");
            } else {
                lore.add(ChatColor.RED + "Clique pour annuler.");
            }
        }

        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }
}
