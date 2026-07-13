package com.janboerman.invsee.spigot.api.response;

import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.SpectatorInventory;

public interface SaveResponse {

    public boolean isSaved();

    public SpectatorInventory<?> getInventory();

    public static SaveResponse saved(SpectatorInventory<?> inventory) {
        return new Saved(inventory);
    }

    public static SaveResponse notSaved(SpectatorInventory<?> inventory) {
        return new NotSaved(inventory);
    }
}

final class Saved implements SaveResponse {

    private final SpectatorInventory<?> inventory;

    Saved(SpectatorInventory<?> inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean isSaved() {
        return true;
    }

    @Override
    public SpectatorInventory<?> getInventory() {
        return inventory;
    }
}

final class NotSaved implements SaveResponse {

    private final SpectatorInventory<?> inventory;

    NotSaved(SpectatorInventory<?> inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean isSaved() {
        return false;
    }

    @Override
    public SpectatorInventory<?> getInventory() {
        return inventory;
    }
}
