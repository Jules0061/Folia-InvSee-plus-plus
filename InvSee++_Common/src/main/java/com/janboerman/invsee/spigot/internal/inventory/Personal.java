package com.janboerman.invsee.spigot.internal.inventory;

import org.bukkit.inventory.InventoryView;

public interface Personal {

    public void watch(InventoryView targetPlayerView);

    public void unwatch();

}
