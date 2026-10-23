package com.ae2udmigrator.ae2fc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.migration.LegacyRewrites;
import appeng.api.migration.LegacyStacks;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import com.ae2udmigrator.Stacks;
import com.ae2udmigrator.Targets;

/**
 * AE2FC's dual and trio interfaces were an item interface with a fluid one, and a gas one, beside it. They are
 * AE2UD's one interface, which holds every kind in the same nine slots. Its fluid assembler is an interface with
 * its patterns, which a molecular assembler beside it crafts.
 */
final class Ae2fcInterfaces {

    private static final String INTERFACE = Ae2fcMigration.AE + "interface";
    private static final String MATERIAL = Ae2fcMigration.AE + "material";
    private static final int CAPACITY_CARD = 27;
    private static final int PATTERN_EXPANSION_CARD = 58;
    private static final int SLOTS = 9;
    private static final int UPGRADE_SLOTS = 4;

    private Ae2fcInterfaces() {
    }

    static void register() {
        for (final String kind : new String[] { "dual", "trio" }) {
            final String block = "ae2fc:" + kind + "_interface";
            if (Targets.block(block, INTERFACE)) {
                LegacyRewrites.addTileRewriter(block, tile -> {
                    final String where = kind + " interface" + Stacks.at(tile);
                    final Dualities old = Dualities.take(tile);
                    tile.setString("id", INTERFACE);
                    final NBTTagCompound rewritten = LegacyRewrites.forwardTile(tile);
                    old.mergeInto(rewritten, where);
                    return rewritten;
                });
            }
            final String part = "ae2fc:part_" + kind + "_interface";
            LegacyRewrites.addStackRewriter(part, stack -> {
                Stacks.become(stack, Ae2fcMigration.AE + "part", 440);
                LegacyRewrites.forwardStack(stack);
            });
            LegacyRewrites.addPartRewriter(part, (item, data) -> {
                final Dualities old = Dualities.take(data);
                Stacks.become(item, Ae2fcMigration.AE + "part", 440);
                LegacyRewrites.forwardPart(item, data);
                old.mergeInto(data, kind + " interface part");
            });
        }

        if (Targets.block("ae2fc:fluid_assembler", INTERFACE)) {
            LegacyRewrites.addTileRewriter("ae2fc:fluid_assembler", Ae2fcInterfaces::assembler);
        }
    }

    /**
     * The patterns move into the interface, and so do the pattern expansion cards that let it use them; a
     * speed card, what it was crafting and what it had not handed back go into a chest beside it.
     */
    private static NBTTagCompound assembler(final NBTTagCompound tile) {
        final String where = "fluid assembler" + Stacks.at(tile);
        final NBTTagList cards = tile.getCompoundTag("upgrade").getTagList("Items", Constants.NBT.TAG_COMPOUND);
        final NBTTagList upgrades = new NBTTagList();
        int expansions = 0;
        for (int i = 0; i < cards.tagCount(); i++) {
            final NBTTagCompound card = cards.getCompoundTagAt(i);
            if (MATERIAL.equals(card.getString("id")) && card.getShort("Damage") == PATTERN_EXPANSION_CARD
                    && upgrades.tagCount() < UPGRADE_SLOTS) {
                final NBTTagCompound kept = card.copy();
                kept.setInteger("Slot", upgrades.tagCount());
                upgrades.appendTag(kept);
                expansions += Math.max(1, Stacks.count(card));
            } else {
                Stacks.spill(card);
            }
        }

        final int room = SLOTS * (1 + Math.min(3, expansions));
        final NBTTagCompound saved = tile.getCompoundTag("inv");
        final NBTTagList patterns = new NBTTagList();
        for (int slot = 0; slot < 36; slot++) {
            final NBTTagCompound pattern = saved.getCompoundTag("item" + slot);
            if (Stacks.isEmpty(pattern)) {
                continue;
            }
            if (patterns.tagCount() >= room) {
                Stacks.spill(pattern);
                continue;
            }
            final NBTTagCompound kept = pattern.copy();
            kept.setInteger("Slot", patterns.tagCount());
            patterns.appendTag(kept);
        }

        final NBTTagList waiting = tile.getTagList("waitingToSend", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < waiting.tagCount(); i++) {
            Stacks.spill(waiting.getCompoundTagAt(i));
        }

        for (final String key : new String[] { "inv", "upgrade", "output", "gridInv", "progress", "myPlan",
                "waitingToSend" }) {
            tile.removeTag(key);
        }
        tile.setTag("patterns", inventory(patterns, 36));
        tile.setTag("upgrades", inventory(upgrades, UPGRADE_SLOTS));
        tile.setBoolean("omniDirectional", true);
        tile.setString("id", INTERFACE);
        LegacyRewrites.log().note(where + " is an interface now, with its " + patterns.tagCount()
                + " patterns; a molecular assembler beside it crafts them");
        return tile;
    }

    private static NBTTagCompound inventory(final NBTTagList items, final int size) {
        final NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", items);
        inventory.setInteger("Size", size);
        return inventory;
    }

    /**
     * What the fluid and gas sides kept, taken out before the item side is rewritten as an interface of UEL's.
     */
    private static final class Dualities {

        private final NBTTagCompound fluid;
        private final NBTTagCompound gas;

        private Dualities(final NBTTagCompound fluid, final NBTTagCompound gas) {
            this.fluid = fluid;
            this.gas = gas;
        }

        static Dualities take(final NBTTagCompound data) {
            final NBTTagCompound item = data.getCompoundTag("itemDuality");
            final Dualities taken = new Dualities(data.getCompoundTag("fluidDuality"),
                    data.getCompoundTag("gasDuality"));
            for (final String key : new String[] { "itemDuality", "fluidDuality", "gasDuality" }) {
                data.removeTag(key);
            }
            for (final String key : item.getKeySet()) {
                data.setTag(key, item.getTag(key));
            }
            // AE2FC's own settings of an item interface; AE2UD has no packets or split blocking modes.
            for (final String key : new String[] { "fluidPacket", "allowSplitting", "blockModeEx" }) {
                data.removeTag(key);
            }
            return taken;
        }

        /**
         * Fluid slots, then gas slots, take the slots the item side left free, each with what it stocked; what
         * finds no free slot is logged. Their cards fill the free card slots and the rest go into a chest.
         */
        void mergeInto(final NBTTagCompound data, final String where) {
            final NBTTagCompound config = data.getCompoundTag("config");
            final NBTTagCompound storage = data.getCompoundTag("storage");
            final List<Slot> slots = new ArrayList<>();
            collect(slots, this.fluid, false, where);
            collect(slots, this.gas, true, where);
            slots.sort((a, b) -> Boolean.compare(a.config == null, b.config == null));

            for (final Slot slot : slots) {
                final int free = freeSlot(config, storage);
                if (free < 0) {
                    if (slot.stock != null) {
                        LegacyRewrites.log().lost(where + ": no free slot for " + slot.stock);
                    } else {
                        LegacyRewrites.log().note(where + ": no free slot for the config of " + slot.config);
                    }
                    continue;
                }
                if (slot.config != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    GenericStack.writeTag(tag, new GenericStack(slot.config, slot.tank));
                    config.setTag("#" + free, tag);
                }
                final NBTTagCompound tag = new NBTTagCompound();
                if (slot.stock != null) {
                    GenericStack.writeTag(tag, slot.stock);
                }
                storage.setTag("#" + free, tag);
            }
            data.setTag("config", config);
            data.setTag("storage", storage);
            mergeCards(data, where);
        }

        private void mergeCards(final NBTTagCompound data, final String where) {
            final NBTTagCompound upgrades = data.getCompoundTag("upgrades");
            final NBTTagList items = upgrades.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            final boolean[] used = new boolean[UPGRADE_SLOTS];
            for (int i = 0; i < items.tagCount(); i++) {
                final int slot = items.getCompoundTagAt(i).getInteger("Slot");
                if (slot >= 0 && slot < UPGRADE_SLOTS) {
                    used[slot] = true;
                }
            }
            for (final NBTTagCompound side : new NBTTagCompound[] { this.fluid, this.gas }) {
                final NBTTagList cards = side.getCompoundTag("upgrades").getTagList("Items",
                        Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < cards.tagCount(); i++) {
                    final NBTTagCompound card = cards.getCompoundTagAt(i).copy();
                    int slot = 0;
                    while (slot < UPGRADE_SLOTS && used[slot]) {
                        slot++;
                    }
                    if (slot == UPGRADE_SLOTS) {
                        Stacks.spill(card);
                        continue;
                    }
                    used[slot] = true;
                    card.setInteger("Slot", slot);
                    items.appendTag(card);
                }
            }
            upgrades.setTag("Items", items);
            upgrades.setInteger("Size", UPGRADE_SLOTS);
            data.setTag("upgrades", upgrades);
        }

        private static void collect(final List<Slot> slots, final NBTTagCompound side, final boolean gas,
                final String where) {
            if (side.isEmpty()) {
                return;
            }
            final int cards = cards(side.getCompoundTag("upgrades"));
            final NBTTagCompound config = side.getCompoundTag("config");
            final NBTTagCompound storage = side.getCompoundTag("storage");
            for (int i = 0; i < SLOTS; i++) {
                final GenericStack configured = read(config.getCompoundTag("#" + i), gas);
                final GenericStack stock = read(storage.getCompoundTag("#" + i), gas);
                if (configured == null && stock == null) {
                    continue;
                }
                final AEKey key = configured == null ? null : configured.what();
                final long capacity = 8 * GenericSlotCapacities.get(
                        (key != null ? key : stock.what()).getType());
                final long tank = Math.min((long) Math.pow(4, cards + 1) * 1000, capacity);
                if (stock != null && stock.amount() > capacity) {
                    LegacyRewrites.log().note(where + ", tank " + i + " holds " + stock.amount()
                            + ", more than the " + capacity + " a slot takes; the interface returns the rest"
                            + " to the network");
                }
                slots.add(new Slot(key, tank, stock != null && stock.amount() > 0 ? stock : null));
            }
        }

        /**
         * A fluid side's slot is an AE fluid stack; a gas side's is a tank, {@code {stored: {gasName, amount}}}.
         */
        @Nullable
        private static GenericStack read(final NBTTagCompound saved, final boolean gas) {
            if (!gas) {
                return saved.isEmpty() ? null : LegacyStacks.read(saved);
            }
            final NBTTagCompound stored = saved.getCompoundTag("stored");
            return stored.hasKey("gasName", Constants.NBT.TAG_STRING)
                    ? Ae2fcMigration.gas(stored.getString("gasName"), stored.getInteger("amount"))
                    : null;
        }

        private static int cards(final NBTTagCompound upgrades) {
            final NBTTagList items = upgrades.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            int count = 0;
            for (int i = 0; i < items.tagCount(); i++) {
                final NBTTagCompound card = items.getCompoundTagAt(i);
                if (MATERIAL.equals(card.getString("id")) && card.getShort("Damage") == CAPACITY_CARD) {
                    count += Math.max(1, card.getInteger("Count"));
                }
            }
            return count;
        }

        private static int freeSlot(final NBTTagCompound config, final NBTTagCompound storage) {
            for (int i = 0; i < SLOTS; i++) {
                if (isFree(config, i) && isFree(storage, i)) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isFree(final NBTTagCompound inventory, final int slot) {
            final NBTTagCompound entry = inventory.getCompoundTag("#" + slot);
            return !entry.hasKey(AEKey.TYPE_FIELD);
        }
    }

    private static final class Slot {
        @Nullable
        private final AEKey config;
        private final long tank;
        @Nullable
        private final GenericStack stock;

        private Slot(@Nullable final AEKey config, final long tank, @Nullable final GenericStack stock) {
            this.config = config;
            this.tank = tank;
            this.stock = stock;
        }
    }
}
