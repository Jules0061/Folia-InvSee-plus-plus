package com.janboerman.invsee.spigot.api;

import javax.annotation.Nullable;

import com.janboerman.invsee.spigot.api.logging.Difference;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.template.Mirror;

public interface SpectatorInventoryView<Slot> {

    public SpectatorInventory<Slot> getTopInventory();

    public @Nullable Difference getTrackedDifference();

    public CreationOptions<Slot> getCreationOptions();

    public String getTitle();

    public Mirror<Slot> getMirror();

    public Target getTarget();

}
