package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.EnderChestSlot;

import org.bukkit.event.inventory.InventoryType;

public interface EnderSpectatorInventoryView extends SpectatorInventoryView<EnderChestSlot> {

    @Override
    public EnderSpectatorInventory getTopInventory();

    public default InventoryType getType() {
        return InventoryType.CHEST;
    }

}
