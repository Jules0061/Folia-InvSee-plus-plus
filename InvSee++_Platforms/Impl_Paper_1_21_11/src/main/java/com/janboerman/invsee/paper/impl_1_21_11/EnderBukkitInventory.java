package com.janboerman.invsee.paper.impl_1_21_11;

import com.janboerman.invsee.spigot.internal.inventory.EnderInventory;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

class EnderBukkitInventory extends CraftInventory implements EnderInventory<EnderNmsInventory, EnderBukkitInventory> {

	protected EnderBukkitInventory(EnderNmsInventory inventory) {
		super(inventory);
	}

	@Override
	public EnderNmsInventory getInventory() {
		return (EnderNmsInventory) super.getInventory();
	}

	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack[] items) {
		HashMap<Integer, ItemStack> leftOvers = new HashMap<>();

		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				ItemStack leftOver = addItem(items[i]);
				if (leftOver != null && leftOver.getAmount() > 0) {
					leftOvers.put(i, leftOver);
				}
			}
		}

		return leftOvers;
	}

	private ItemStack addItem(ItemStack itemStack) {
		if (itemStack == null || itemStack.getAmount() == 0) return null;

		ItemStack[] storageContents = getStorageContents();
		addItem(storageContents, itemStack, getMaxStackSize());
		setStorageContents(storageContents);

		return itemStack;
	}

	private static void addItem(final ItemStack[] contents, final ItemStack add, final int inventoryMaxStackSize) {
		assert contents != null && add != null;

		for (int i = 0; i < contents.length && add.getAmount() > 0; i++) {
			final ItemStack existingStack = contents[i];
			if (add.isSimilar(existingStack)) {
				final int maxStackSizeForThisItem = Math.min(inventoryMaxStackSize, ItemUtils.getMaxStackSize(existingStack));
				if (existingStack.getAmount() < maxStackSizeForThisItem) {

					final int maxMergeAmount = Math.min(maxStackSizeForThisItem - existingStack.getAmount(), add.getAmount());
					if (maxMergeAmount > 0) {
						if (add.getAmount() <= maxMergeAmount) {

							existingStack.setAmount(existingStack.getAmount() + add.getAmount());
							add.setAmount(0);
						} else {

							assert maxStackSizeForThisItem == existingStack.getAmount() + maxMergeAmount;
							existingStack.setAmount(maxStackSizeForThisItem);
							add.setAmount(add.getAmount() - maxMergeAmount);
						}
					}
				}
			}
		}

		final int maxStackSizeForThisItem = Math.min(inventoryMaxStackSize, Math.min(ItemUtils.getMaxStackSize(add), add.getAmount()));
		for (int i = 0; i < contents.length && add.getAmount() > 0; i++) {
			if (ItemUtils.isEmpty(contents[i])) {
				if (add.getAmount() <= maxStackSizeForThisItem) {

					contents[i] = add.clone();
					add.setAmount(0);
				} else {

					ItemStack clone = add.clone(); clone.setAmount(maxStackSizeForThisItem);
					contents[i] = clone;
					add.setAmount(add.getAmount() - maxStackSizeForThisItem);
				}
			}
		}
	}

}
