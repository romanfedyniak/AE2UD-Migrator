package com.ae2udmigrator.ae2fc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import appeng.api.migration.LegacyRewrites;
import appeng.api.migration.LegacyStacks;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;

import com.ae2udmigrator.Stacks;
import com.ae2udmigrator.Targets;

/**
 * AE2FluidCraft-Rework (the unofficial 2.7 line): AE2UD carries fluids and gases where it carries items, so its
 * fluid patterns, terminals and dual interfaces become AE2UD's own, and the items it wrapped fluids and gases in
 * become the stacks they stood for.
 */
public final class Ae2fcMigration {

    static final String AE = "appliedenergistics2:";
    private static final String LEVEL_MAINTAINER = "threng:machine";

    private Ae2fcMigration() {
    }

    public static void register() {
        LegacyStacks.addWrapper("ae2fc:fluid_drop", stack -> {
            final NBTTagCompound tag = stack.getCompoundTag("tag");
            final AEFluidKey fluid = LegacyStacks.fluid(tag.getString("Fluid"), tag, "FluidTag");
            return fluid == null ? null : new GenericStack(fluid, Stacks.count(stack));
        });
        LegacyStacks.addWrapper("ae2fc:fluid_packet", stack -> {
            final NBTTagCompound fluidStack = stack.getCompoundTag("tag").getCompoundTag("FluidStack");
            final AEFluidKey fluid = LegacyStacks.fluid(fluidStack.getString("FluidName"), fluidStack, "Tag");
            return fluid == null ? null
                    : new GenericStack(fluid, (long) fluidStack.getInteger("Amount") * Math.max(1, Stacks.count(stack)));
        });
        LegacyStacks.addWrapper("ae2fc:gas_drop",
                stack -> gas(stack.getCompoundTag("tag").getString("Gas"), Stacks.count(stack)));
        LegacyStacks.addWrapper("ae2fc:gas_packet", stack -> {
            final NBTTagCompound gasStack = stack.getCompoundTag("tag").getCompoundTag("GasStack");
            return gas(gasStack.getString("gasName"), (long) gasStack.getInteger("amount") * Math.max(1, Stacks.count(stack)));
        });

        Ae2fcPatterns.register();
        Ae2fcInterfaces.register();

        LegacyRewrites.addStackRewriter("ae2fc:wireless_fluid_pattern_terminal", stack -> {
            final NBTTagCompound tag = stack.getCompoundTag("tag");
            tag.removeTag("combine");
            tag.removeTag("fluidFirst");
            stack.setString("id", AE + "wireless_pattern_terminal");
            LegacyRewrites.forwardStack(stack);
        });
        terminal("ae2fc:part_fluid_pattern_terminal", 340);
        terminal("ae2fc:part_fluid_pattern_ex_terminal", 341);

        levelMaintainer();

        LegacyRewrites.remove("ae2fc:fluid_discretizer");
        LegacyRewrites.remove("ae2fc:gas_discretizer");
        removed("ae2fc:fluid_pattern_encoder", "Inventory");
        removed("ae2fc:fluid_packet_decoder", "Inventory");
        removed("ae2fc:ultimate_encoder", null);
        removed("ae2fc:ingredient_buffer", "ItemInv");
        removed("ae2fc:large_ingredient_buffer", "ItemInv");
        removed("ae2fc:burette", "ItemInv");
    }

    /**
     * A gas as MekEng reads one UEL kept; null without MekEng.
     */
    static GenericStack gas(final String name, final long amount) {
        final NBTTagCompound saved = new NBTTagCompound();
        saved.setString("gasName", name);
        saved.setLong("Cnt", amount);
        return name.isEmpty() ? null : LegacyStacks.read(saved);
    }

    /**
     * AE2FC's pattern terminals were UEL's with two settings of their own, and become what UEL's do.
     */
    private static void terminal(final String id, final int uelMeta) {
        LegacyRewrites.addStackRewriter(id, stack -> {
            Stacks.become(stack, AE + "part", uelMeta);
            LegacyRewrites.forwardStack(stack);
        });
        LegacyRewrites.addPartRewriter(id, (item, part) -> {
            part.removeTag("combineMode");
            part.removeTag("fluidFirst");
            Stacks.become(item, AE + "part", uelMeta);
            LegacyRewrites.forwardPart(item, part);
        });
    }

    /**
     * Five rows of a fluid to keep stocked, the way Lazy AE2's level maintainer keeps any key stocked.
     */
    private static void levelMaintainer() {
        final String id = "ae2fc:fluid_level_maintainer";
        if (!Targets.block(id, LEVEL_MAINTAINER, 4)) {
            return;
        }
        LegacyRewrites.addTileRewriter(id, tile -> {
            final NBTTagCompound rows = tile.getCompoundTag("configX");
            final NBTTagCompound inventory = new NBTTagCompound();
            for (int row = 0; row < 5; row++) {
                final NBTTagCompound saved = rows.getCompoundTag("#" + row);
                final GenericStack read = LegacyStacks.read(saved);
                if (read == null) {
                    if (!saved.isEmpty()) {
                        LegacyRewrites.log().lost("level maintainer" + Stacks.at(tile) + ", row " + row + ": " + saved);
                    }
                    continue;
                }
                inventory.setTag("item" + row, LegacyStacks.wrapped(new GenericStack(read.what(), 1)));
                tile.setLong("target" + row, read.amount());
                tile.setLong("batch" + row, tile.getLong("reqX" + row));
            }
            tile.setTag("inv", inventory);
            final EnumFacing front = EnumFacing.byHorizontalIndex(tile.getInteger("facing"));
            tile.setString("front", front.getName());
            for (final String key : new String[] { "configX", "facing", "reqX0", "reqX1", "reqX2", "reqX3", "reqX4",
                    "links-0", "links-1", "links-2", "links-3", "links-4", "forward", "up" }) {
                tile.removeTag(key);
            }
            tile.setString("id", "threng:TileLevelMaintainer");
            return tile;
        });
    }

    /**
     * A machine with no equivalent: the block is gone, and the items it held wait in a chest beside where it
     * stood. Fluids it held are logged.
     */
    private static void removed(final String id, final String inventoryKey) {
        LegacyRewrites.remove(id);
        LegacyRewrites.addTileRewriter(id, tile -> {
            if (inventoryKey != null) {
                Stacks.spillItems(tile.getCompoundTag(inventoryKey));
            } else {
                Stacks.spillSlots(tile.getCompoundTag("inv"));
            }
            final NBTTagCompound tanks = tile.getCompoundTag("FluidInv");
            for (final String key : tanks.getKeySet()) {
                final NBTTagCompound tank = tanks.getCompoundTag(key);
                if (!tank.isEmpty()) {
                    LegacyRewrites.log().lost(id + Stacks.at(tile) + ", tank " + key + ": " + tank);
                }
            }
            return tile;
        });
    }
}
