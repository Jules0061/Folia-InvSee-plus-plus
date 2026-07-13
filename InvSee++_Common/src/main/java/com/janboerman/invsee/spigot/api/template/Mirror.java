package com.janboerman.invsee.spigot.api.template;

import com.janboerman.invsee.spigot.internal.template.EnderChestMirror;
import com.janboerman.invsee.spigot.internal.template.PlayerInventoryMirror;

public interface Mirror<Slot> {

    public Integer getIndex(Slot slot);

    public Slot getSlot(int index);

    public static Mirror<PlayerInventorySlot> defaultPlayerInventory() {
        return DefaultMirrors.DEFAULT_PLAYERINVENTORY_MIRROR;
    }

    public static Mirror<EnderChestSlot> defaultEnderChest() {
        return DefaultMirrors.DEFAULT_ENDERCHEST_MIRROR;
    }

    public static Mirror<PlayerInventorySlot> forInventory(String template) {
        return PlayerInventoryMirror.ofTemplate(template);
    }

    public static Mirror<EnderChestSlot> forEnderChest(String template) {
        return EnderChestMirror.ofTemplate(template);
    }

    public static String toInventoryTemplate(Mirror<PlayerInventorySlot> mirror) {
        if (mirror == DefaultMirrors.DEFAULT_PLAYERINVENTORY_MIRROR) {
            return PlayerInventoryMirror.DEFAULT_TEMPLATE;
        } else {
            return PlayerInventoryMirror.toTemplate(mirror);
        }
    }

    public static String toEnderChestTemplate(Mirror<EnderChestSlot> mirror) {
        if (mirror == DefaultMirrors.DEFAULT_ENDERCHEST_MIRROR) {
            return EnderChestMirror.DEFAULT_TEMPLATE;
        } else {
            return EnderChestMirror.toTemplate(mirror);
        }
    }
}

class DefaultMirrors {

    private DefaultMirrors() {}

    static final Mirror<PlayerInventorySlot> DEFAULT_PLAYERINVENTORY_MIRROR = new Mirror<PlayerInventorySlot>() {
        @Override public Integer getIndex(PlayerInventorySlot playerInventorySlot) {
            if (playerInventorySlot == null) return null;
            return playerInventorySlot.defaultIndex();
        }

        @Override public PlayerInventorySlot getSlot(int index) {
            return PlayerInventorySlot.byDefaultIndex(index);
        }
    };

    static final Mirror<EnderChestSlot> DEFAULT_ENDERCHEST_MIRROR = new Mirror<EnderChestSlot>() {
        @Override public Integer getIndex(EnderChestSlot enderChestSlot) {
            if (enderChestSlot == null) return null;
            return enderChestSlot.defaultIndex();
        }

        @Override public EnderChestSlot getSlot(int index) {
            return EnderChestSlot.byDefaultIndex(index);
        }
    };

}
