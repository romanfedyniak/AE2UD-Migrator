package com.ae2udmigrator.wut;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import appeng.api.migration.LegacyRewrites;
import appeng.helpers.PatternHelper;

/**
 * The wireless universal terminal: AE2UD's wireless terminal holds several modes the same way, but names them,
 * where this one numbered them, and keeps each mode's grid apart.
 */
public final class WutMigration {

    private static final String AE = "appliedenergistics2:";
    private static final String TERMINAL = AE + "terminal";
    private static final String CRAFTING = AE + "crafting_terminal";
    private static final String PATTERN = AE + "pattern_terminal";

    /**
     * Its modes by number. Six to nine were the extended crafting terminals of another mod, kept under the
     * names of that mod's own wireless terminals, which AE2UD keeps until something registers them.
     */
    private static final String[] MODES = { null, CRAFTING, TERMINAL, PATTERN, PATTERN, TERMINAL,
            "ae2exttable:wireless_basic_crafting_terminal", "ae2exttable:wireless_advanced_crafting_terminal",
            "ae2exttable:wireless_elite_crafting_terminal", "ae2exttable:wireless_ultimate_crafting_terminal",
            AE + "pattern_access_terminal" };

    private WutMigration() {
    }

    public static void register() {
        LegacyRewrites.addStackRewriter("ae2wut:wireless_universal_terminal", WutMigration::rewrite);
    }

    private static void rewrite(final NBTTagCompound stack) {
        stack.setString("id", AE + "wireless_terminal");
        stack.setShort("Damage", (short) 0);
        if (!stack.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        final NBTTagCompound tag = stack.getCompoundTag("tag");
        final int current = tag.hasKey("mode", Constants.NBT.TAG_ANY_NUMERIC) ? tag.getInteger("mode") : 0;
        final NBTTagCompound cache = tag.getCompoundTag("cache");

        final Set<String> unlocked = new LinkedHashSet<>();
        for (final int number : tag.getIntArray("modes")) {
            final String mode = mode(number);
            if (mode == null) {
                LegacyRewrites.log().note("wireless universal terminal: mode " + number + " is unknown, dropped");
                continue;
            }
            unlocked.add(mode);
        }

        final NBTTagCompound modeData = new NBTTagCompound();
        if (unlocked.contains(CRAFTING)) {
            final NBTTagCompound grid = grid(tag, cache, current, 1);
            if (grid != null) {
                modeData.setTag(CRAFTING, data("craftingGrid", grid));
            }
        }
        if (unlocked.contains(PATTERN)) {
            modeData.setTag(PATTERN, patternMode(tag, cache, current));
        }
        for (int number = 6; number <= 9; number++) {
            final NBTTagCompound grid = number == current ? null : grid(tag, cache, current, number);
            final NBTBase open = number == current ? tag.getTag("crafting") : null;
            if (grid != null) {
                modeData.setTag(MODES[number], data("crafting", grid.getTag("Items")));
            } else if (open != null) {
                modeData.setTag(MODES[number], data("crafting", open));
            }
        }

        final NBTTagList ghost = tag.getCompoundTag("craftingGrid").getTagList("Items", Constants.NBT.TAG_COMPOUND);
        if (ghost.tagCount() > 0 && current != 1 && current != 3 && current != 4) {
            LegacyRewrites.log().lost("wireless universal terminal, a grid of no open mode: " + ghost);
        }

        for (final String key : new String[] { "mode", "modes", "cache", "craft", "Nova", "craftingGrid", "output",
                "patterns", "crafting", "combine", "fluidFirst" }) {
            tag.removeTag(key);
        }
        if (!modeData.isEmpty()) {
            tag.setTag("modeData", modeData);
        }
        final NBTTagList modes = new NBTTagList();
        for (final String mode : unlocked) {
            modes.appendTag(new NBTTagString(mode));
        }
        tag.setTag("modes", modes);
        final String open = mode(current);
        if (open != null && unlocked.contains(open)) {
            tag.setString("mode", open);
        }
    }

    private static String mode(final int number) {
        return number > 0 && number < MODES.length ? MODES[number] : null;
    }

    /**
     * One pattern mode is kept, the open one if that was a pattern mode, and its grid; the slots for blank and
     * encoded patterns were the same for both. A processing recipe moves to AE2UD's processing grid.
     */
    private static NBTTagCompound patternMode(final NBTTagCompound tag, final NBTTagCompound cache, final int current) {
        final int source = current == 3 || current == 4 ? current
                : grid(tag, cache, current, 3) != null ? 3 : 4;
        final NBTTagCompound data = new NBTTagCompound();
        final NBTTagCompound grid = grid(tag, cache, current, source);
        final boolean crafting = tag.getBoolean("isCraftingMode");
        if (grid != null) {
            if (crafting) {
                data.setTag("craftingGrid", grid);
            } else {
                data.setTag("processing", processing(grid));
            }
        }
        for (final String key : new String[] { "output", "patterns" }) {
            if (tag.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
                data.setTag(key, tag.getCompoundTag(key));
            }
        }
        tag.setString("encodingMode", AE + (crafting ? "crafting" : "processing"));
        tag.removeTag("isCraftingMode");
        return data;
    }

    /**
     * The open mode's grid is the terminal's own; the others wait in the cache under their number.
     */
    private static NBTTagCompound grid(final NBTTagCompound tag, final NBTTagCompound cache, final int current,
            final int number) {
        if (number == current) {
            return tag.hasKey("craftingGrid", Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag("craftingGrid") : null;
        }
        if (!cache.hasKey(String.valueOf(number), Constants.NBT.TAG_LIST)) {
            return null;
        }
        final NBTTagCompound grid = new NBTTagCompound();
        grid.setTag("Items", cache.getTagList(String.valueOf(number), Constants.NBT.TAG_COMPOUND));
        grid.setInteger("Size", 9);
        return grid;
    }

    /**
     * UEL's pattern terminal kept a processing recipe in its three by three grid, row by row.
     */
    private static NBTTagCompound processing(final NBTTagCompound grid) {
        final NBTTagList items = grid.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        final NBTTagList moved = new NBTTagList();
        for (int i = 0; i < items.tagCount(); i++) {
            final NBTTagCompound item = items.getCompoundTagAt(i).copy();
            final int slot = item.getInteger("Slot");
            item.setInteger("Slot", slot / 3 * PatternHelper.PROCESSING_INPUT_WIDTH + slot % 3);
            moved.appendTag(item);
        }
        final NBTTagCompound processing = new NBTTagCompound();
        processing.setTag("Items", moved);
        processing.setInteger("Size", PatternHelper.PROCESSING_INPUT_LIMIT);
        return processing;
    }

    private static NBTTagCompound data(final String key, final NBTBase value) {
        final NBTTagCompound data = new NBTTagCompound();
        data.setTag(key, value);
        return data;
    }
}
