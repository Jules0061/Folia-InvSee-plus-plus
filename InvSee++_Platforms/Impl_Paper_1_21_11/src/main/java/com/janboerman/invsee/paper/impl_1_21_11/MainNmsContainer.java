package com.janboerman.invsee.paper.impl_1_21_11;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.logging.DifferenceTracker;
import com.janboerman.invsee.spigot.api.logging.LogOptions;
import com.janboerman.invsee.spigot.api.logging.LogOutput;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

class MainNmsContainer extends AbstractContainerMenu {

	final Player player;
	final MainNmsInventory top;
	final Inventory bottom;
	final String originalTitle;
	String title;

	final CreationOptions<PlayerInventorySlot> creationOptions;
	private final boolean spectatingOwnInventory;
	private MainBukkitInventoryView bukkitView;
	final DifferenceTracker tracker;

	private static Slot makeSlot(Mirror<PlayerInventorySlot> mirror, boolean spectatingOwnInventory, MainNmsInventory top, int positionIndex, int magicX, int magicY,
								 ItemStack inaccessiblePlaceholder) {
		final PlayerInventorySlot place = mirror.getSlot(positionIndex);

		if (place == null) {
			return new InaccessibleSlot(inaccessiblePlaceholder, top, positionIndex, magicX, magicY);
		} else if (place.isContainer()) {
			final int referringTo = place.ordinal() - PlayerInventorySlot.CONTAINER_00.ordinal();
			return new Slot(top, referringTo, magicX, magicY);
		} else if (place == PlayerInventorySlot.ARMOUR_BOOTS) {
			final int referringTo = 36;
			return new BootsSlot(top, referringTo, magicX, magicY);
		} else if (place == PlayerInventorySlot.ARMOUR_LEGGINGS) {
			final int referringTo = 37;
			return new LeggingsSlot(top, referringTo, magicX, magicY);
		} else if (place == PlayerInventorySlot.ARMOUR_CHESTPLATE) {
			final int referringTo = 38;
			return new ChestplateSlot(top, referringTo, magicX, magicY);
		} else if (place == PlayerInventorySlot.ARMOUR_HELMET) {
			final int referringTo = 39;
			return new HelmetSlot(top, referringTo, magicX, magicY);
		} else if (place.isOffHand()) {
			final int referringTo = 40;
			return new OffhandSlot(top, referringTo, magicX, magicY);
		} else if (place.isBody()) {
			final int referringTo = 41;
			return new BodySlot(top, referringTo, magicX, magicY);
		} else if (place.isSaddle()) {
			final int referringTo = 42;
			return new SaddleSlot(top, referringTo, magicX, magicY);
		} else if (place.isCursor() && !spectatingOwnInventory) {
			final int referringTo = 43;
			return new CursorSlot(top, referringTo, magicX, magicY);
		} else if (place.isPersonal()) {
			final int referringTo = place.ordinal() - PlayerInventorySlot.PERSONAL_00.ordinal() + 45;
			return new PersonalSlot(inaccessiblePlaceholder, top, referringTo, magicX, magicY);
		} else {
			return new InaccessibleSlot(inaccessiblePlaceholder, top, positionIndex, magicX, magicY);
		}
	}

	@Override
	public void clicked(int i, int j, ClickType inventoryclicktype, Player entityhuman) {

		List<org.bukkit.inventory.ItemStack> contentsBefore = null, contentsAfter;
		if (tracker != null) {
			contentsBefore = top.getContents().stream().map(CraftItemStack::asBukkitCopy).toList();
		}

		super.clicked(i, j, inventoryclicktype, entityhuman);

		if (tracker != null) {
			contentsAfter = top.getContents().stream().map(CraftItemStack::asBukkitCopy).toList();
			tracker.onClick(contentsBefore, contentsAfter);
		}
	}

	@Override
	public void removed(Player entityhuman) {
		super.removed(entityhuman);

		if (tracker != null && Objects.equals(entityhuman, player)) {
			tracker.onClose();
		}
	}

	MainNmsContainer(int id, MainNmsInventory nmsInventory, Inventory bottomInventory, Player spectator, CreationOptions<PlayerInventorySlot> creationOptions) {
		super(MenuType.GENERIC_9x6, id);

		this.top = nmsInventory;
		this.bottom = bottomInventory;
		this.player = spectator;
		this.spectatingOwnInventory = spectator.getUUID().equals(nmsInventory.targetPlayerUuid);

		this.creationOptions = creationOptions;
		Target target = Target.byGameProfile(nmsInventory.targetPlayerUuid, nmsInventory.targetPlayerName);
		this.originalTitle = creationOptions.getTitle().titleFor(target);
		Mirror<PlayerInventorySlot> mirror = creationOptions.getMirror();
		LogOptions logOptions = creationOptions.getLogOptions();
		Plugin plugin = creationOptions.getPlugin();
		if (!LogOptions.isEmpty(logOptions)) {
			this.tracker = new DifferenceTracker(
					LogOutput.make(plugin, player.getUUID(), player.getScoreboardName(), target, logOptions),
					logOptions.getGranularity());
			this.tracker.onOpen();
		} else {
			this.tracker = null;
		}
		ItemStack inaccessibleSlotPlaceholder = CraftItemStack.asNMSCopy(creationOptions.getPlaceholderPalette().inaccessible());

		for (int yPos = 0; yPos < 6; yPos++) {
			for (int xPos = 0; xPos < 9; xPos++) {
				int index = xPos + yPos * 9;
				int magicX = 8 + xPos * 18;
				int magicY = 18 + yPos * 18;

				addSlot(makeSlot(mirror, spectatingOwnInventory, top, index, magicX, magicY, inaccessibleSlotPlaceholder));
			}
		}

		int magicAddY = (6  - 4 ) * 18;

		for (int yPos = 1; yPos < 4; yPos++) {
			for (int xPos = 0; xPos < 9; xPos++) {
				int index = xPos + yPos * 9;
				int magicX = 8 + xPos * 18;
				int magicY = 103 + yPos * 18 + magicAddY;
				addSlot(new Slot(bottomInventory, index, magicX, magicY));
			}
		}

		for (int xPos = 0; xPos < 9; xPos++) {
			int index = xPos;
			int magicX = 8 + xPos * 18;
			int magicY = 161 + magicAddY;
			addSlot(new Slot(bottomInventory, index, magicX, magicY));
		}
	}

	@Override
	public MainBukkitInventoryView getBukkitView() {
		if (bukkitView == null) {
			bukkitView = new MainBukkitInventoryView(this);
		}
		return bukkitView;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player entityHuman, int rawIndex) {

		if (spectatingOwnInventory)
			return ItemStack.EMPTY;

		ItemStack itemStack = ItemStack.EMPTY;
		final Slot slot = getSlot(rawIndex);
		final int topRows = 6;

		if (slot != null && slot.hasItem()) {
			ItemStack clickedSlotItem = slot.getItem();

			itemStack = clickedSlotItem.copy();
			if (rawIndex < topRows * 9) {

				if (!moveItemStackTo(clickedSlotItem, topRows * 9, slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {

				if (!moveItemStackTo(clickedSlotItem, 0, topRows * 9, false)) {
					return ItemStack.EMPTY;
				}
			}

			if (clickedSlotItem.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemStack;
	}

	public String title() {
		return title != null ? title : originalTitle;
	}

}
