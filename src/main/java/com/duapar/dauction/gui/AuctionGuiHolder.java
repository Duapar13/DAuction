package com.duapar.dauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class AuctionGuiHolder implements InventoryHolder {

    public enum Mode {
        BROWSE,
        MY,
        RECLAIM
    }

    private final Mode mode;
    private final Map<Integer, Integer> slotToListingId = new HashMap<>();
    private Inventory inventory;

    public AuctionGuiHolder(Mode mode) {
        this.mode = mode;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Mode getMode() {
        return mode;
    }

    public void put(int slot, int listingId) {
        slotToListingId.put(slot, listingId);
    }

    public Integer getListingId(int slot) {
        return slotToListingId.get(slot);
    }
}
