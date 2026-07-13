package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import org.bukkit.inventory.ItemStack;

public interface EnderSpectatorInventory extends SpectatorInventory<EnderChestSlot> {

    @Override
    public default Mirror<EnderChestSlot> getMirror() {
        return getCreationOptions().getMirror();
    }

    public default void setContents(EnderSpectatorInventory newContents) {
        setContents(newContents.getContents());
    }

    @Override
    public default void setStorageContents(ItemStack[] newContents) {
        setContents(newContents);
    }

    @Override
    public default ItemStack[] getStorageContents() {
        return getContents();
    }

}
