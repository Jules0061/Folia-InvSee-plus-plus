package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;

import org.bukkit.event.inventory.InventoryType;

public interface MainSpectatorInventoryView extends SpectatorInventoryView<PlayerInventorySlot> {

    @Override
    public MainSpectatorInventory getTopInventory();

    public default InventoryType getType() {
        return InventoryType.CHEST;
    }

}
