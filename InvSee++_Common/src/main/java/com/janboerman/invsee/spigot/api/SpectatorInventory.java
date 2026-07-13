package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.template.Mirror;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public interface SpectatorInventory<Slot> extends Inventory {

    public String getSpectatedPlayerName();

    public UUID getSpectatedPlayerId();

    public String getTitle();

    public Mirror<Slot> getMirror();

    public CreationOptions<Slot> getCreationOptions();

}
