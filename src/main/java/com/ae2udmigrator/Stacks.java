package com.ae2udmigrator;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import appeng.api.migration.LegacyRewrites;
import appeng.api.stacks.GenericStack;

/**
 * Saved stacks and inventories, as NBT.
 */
public final class Stacks {

    private Stacks() {
    }

    /**
     * Another item and metadata, count and tag kept.
     */
    public static void become(final NBTTagCompound stack, final String id, final int meta) {
        stack.setString("id", id);
        stack.setShort("Damage", (short) meta);
    }

    /**
     * A stack with nothing to become: logged, and read as nothing.
     */
    public static void lose(final NBTTagCompound stack) {
        LegacyRewrites.log().lost("item " + stack);
        clear(stack);
    }

    public static void clear(final NBTTagCompound stack) {
        for (final String key : new ArrayList<>(stack.getKeySet())) {
            stack.removeTag(key);
        }
        stack.setString("id", "minecraft:air");
        stack.setByte("Count", (byte) 0);
    }

    public static boolean isEmpty(final NBTTagCompound stack) {
        return stack.isEmpty() || "minecraft:air".equals(stack.getString("id")) || count(stack) <= 0;
    }

    /**
     * AE2 kept a count past what a byte holds in {@code stackSize}.
     */
    public static int count(final NBTTagCompound stack) {
        return stack.hasKey("stackSize") ? stack.getInteger("stackSize") : stack.getInteger("Count");
    }

    /**
     * Every stack of an {@code {Items: [...]}} inventory into the chest beside the tile being rewritten.
     */
    public static void spillItems(final NBTTagCompound inventory) {
        final NBTTagList items = inventory.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            spill(items.getCompoundTagAt(i));
        }
    }

    /**
     * Every stack of an {@code {item0: ..., item1: ...}} inventory into the chest beside the tile being rewritten.
     */
    public static void spillSlots(final NBTTagCompound inventory) {
        for (final String key : inventory.getKeySet()) {
            if (inventory.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
                spill(inventory.getCompoundTag(key));
            }
        }
    }

    /**
     * A stack into the chest beside the tile being rewritten, in pieces a chest slot holds.
     */
    public static void spill(final NBTTagCompound saved) {
        if (isEmpty(saved)) {
            return;
        }
        int left = count(saved);
        final NBTTagCompound one = saved.copy();
        one.removeTag("Slot");
        one.removeTag("stackSize");
        one.setByte("Count", (byte) 1);
        // Read once, so that the piece size is the item's own once it has been renamed and rewritten.
        final ItemStack read = new ItemStack(one.copy());
        if (read.isEmpty()) {
            return;
        }
        // A fluid or gas in an item slot would vanish from a player's inventory.
        if (GenericStack.isWrapped(read)) {
            LegacyRewrites.log().lost("stack with nowhere to go: " + saved);
            return;
        }
        final int piece = Math.max(1, read.getMaxStackSize());
        while (left > 0) {
            final NBTTagCompound part = read.writeToNBT(new NBTTagCompound());
            part.setByte("Count", (byte) Math.min(piece, left));
            LegacyRewrites.spill(part);
            left -= piece;
        }
    }

    public static String at(final NBTTagCompound tile) {
        return " at " + tile.getInteger("x") + " " + tile.getInteger("y") + " " + tile.getInteger("z");
    }
}
