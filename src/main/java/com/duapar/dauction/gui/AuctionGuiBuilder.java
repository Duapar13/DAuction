package com.duapar.dauction.gui;

import com.duapar.dauction.manager.AuctionManager;
import com.duapar.dauction.model.AuctionListing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class AuctionGuiBuilder {

    private AuctionGuiBuilder() {
    }

    public static Inventory buildBrowseGui(List<AuctionListing> listings, AuctionManager manager, UUID viewerId,
                                            Function<AuctionListing, String> factionLineProvider) {
        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.Mode.BROWSE);
        Inventory inventory = Bukkit.createInventory(holder, 54, ChatColor.DARK_PURPLE + "Hôtel des ventes");
        holder.setInventory(inventory);
        fill(inventory, holder, listings, 54,
                listing -> AuctionItemFactory.buildBrowseItem(listing, manager, factionLineProvider.apply(listing),
                        listing.getSellerId().equals(viewerId)));
        return inventory;
    }

    public static Inventory buildMyGui(List<AuctionListing> listings, AuctionManager manager) {
        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.Mode.MY);
        int size = sizeFor(listings.size());
        Inventory inventory = Bukkit.createInventory(holder, size, ChatColor.DARK_PURPLE + "Mes annonces actives");
        holder.setInventory(inventory);
        fill(inventory, holder, listings, size, listing -> AuctionItemFactory.buildMyItem(listing, manager));
        return inventory;
    }

    public static Inventory buildReclaimGui(List<AuctionListing> listings, AuctionManager manager) {
        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.Mode.RECLAIM);
        int size = sizeFor(listings.size());
        Inventory inventory = Bukkit.createInventory(holder, size, ChatColor.DARK_PURPLE + "Objets à récupérer");
        holder.setInventory(inventory);
        fill(inventory, holder, listings, size, listing -> AuctionItemFactory.buildReclaimItem(listing, manager));
        return inventory;
    }

    private static int sizeFor(int count) {
        return Math.min(54, Math.max(9, ((count + 8) / 9) * 9));
    }

    private static void fill(Inventory inventory, AuctionGuiHolder holder, List<AuctionListing> listings,
                              int size, Function<AuctionListing, ItemStack> factory) {
        int slot = 0;
        for (AuctionListing listing : listings) {
            if (slot >= size) {
                break;
            }
            inventory.setItem(slot, factory.apply(listing));
            holder.put(slot, listing.getId());
            slot++;
        }
    }
}
