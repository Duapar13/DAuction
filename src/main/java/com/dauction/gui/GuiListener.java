package com.dauction.gui;

import com.dauction.manager.AuctionException;
import com.dauction.manager.AuctionManager;
import com.dauction.model.AuctionListing;
import com.dauction.util.Display;
import com.dauction.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GuiListener implements Listener {

    private final AuctionManager auctionManager;

    public GuiListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuctionGuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        AuctionGuiHolder holder = (AuctionGuiHolder) event.getInventory().getHolder();
        Integer listingId = holder.getListingId(event.getSlot());
        if (listingId == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        switch (holder.getMode()) {
            case BROWSE:
                handleBrowseClick(player, listingId);
                break;
            case MY:
                handleCancel(player, listingId);
                break;
            case RECLAIM:
                handleCollect(player, listingId);
                break;
        }
    }

    /**
     * En mode BROWSE, cliquer sur sa propre annonce l'annule (au lieu de tenter un
     * achat qui échouerait de toute façon avec "Tu ne peux pas acheter ta propre
     * annonce.") - même comportement que le mode MY, juste accessible sans changer
     * de GUI.
     */
    private void handleBrowseClick(Player player, int id) {
        try {
            AuctionListing listing = auctionManager.getActiveOrThrow(id);
            if (listing.getSellerId().equals(player.getUniqueId())) {
                handleCancel(player, id);
            } else {
                handleBuy(player, id);
            }
        } catch (AuctionException e) {
            Msg.error(player, e.getMessage());
        }
    }

    private void handleBuy(Player player, int id) {
        try {
            AuctionListing listing = auctionManager.buy(player, id);
            giveItem(player, listing.getItem());
            player.closeInventory();
            Msg.success(player, "Tu as acheté " + Display.itemName(listing.getItem()) + " pour "
                    + auctionManager.format(listing.getPrice()) + ".");

            Player seller = Bukkit.getPlayer(listing.getSellerId());
            if (seller != null) {
                Msg.send(seller, player.getName() + " a acheté ton annonce #" + id + " ("
                        + Display.itemName(listing.getItem()) + ") pour " + auctionManager.format(listing.getPrice()) + ".");
            }
        } catch (AuctionException e) {
            Msg.error(player, e.getMessage());
        }
    }

    private void handleCancel(Player player, int id) {
        try {
            auctionManager.cancel(player.getUniqueId(), player.hasPermission("dauction.admin"), id);
            player.closeInventory();
            Msg.success(player, "Annonce #" + id + " annulée. Récupère l'objet via /ah reclaim.");
        } catch (AuctionException e) {
            Msg.error(player, e.getMessage());
        }
    }

    private void handleCollect(Player player, int id) {
        try {
            AuctionListing listing = auctionManager.collect(player.getUniqueId(), id);
            giveItem(player, listing.getItem());
            player.closeInventory();
            Msg.success(player, "Tu as récupéré " + Display.itemName(listing.getItem()) + ".");
        } catch (AuctionException e) {
            Msg.error(player, e.getMessage());
        }
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
    }
}
