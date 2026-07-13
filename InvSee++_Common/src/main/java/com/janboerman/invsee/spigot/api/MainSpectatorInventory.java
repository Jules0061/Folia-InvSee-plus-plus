package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import org.bukkit.inventory.ItemStack;

public interface MainSpectatorInventory extends SpectatorInventory<PlayerInventorySlot> {

    ItemStack[] getStorageContents();

    void setStorageContents(ItemStack[] storageContents);

    ItemStack[] getArmourContents();

    void setArmourContents(ItemStack[] armourContents);

    ItemStack[] getOffHandContents();

    void setOffHandContents(ItemStack[] offHand);

    void setCursorContents(ItemStack cursor);

    ItemStack getCursorContents();

    void setPersonalContents(ItemStack[] craftingContents);

    ItemStack[] getPersonalContents();

    int getPersonalContentsSize();

    public default Mirror<PlayerInventorySlot> getMirror() {
        return getCreationOptions().getMirror();
    }

    public default void setContents(MainSpectatorInventory newContents) {
        setStorageContents(newContents.getStorageContents());
        setArmourContents(newContents.getArmourContents());
        setOffHandContents(newContents.getOffHandContents());
        setCursorContents(newContents.getCursorContents());
        setPersonalContents(newContents.getPersonalContents());
    }

}
