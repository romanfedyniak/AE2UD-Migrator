package com.ae2udmigrator.netvis;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import appeng.api.migration.LegacyRewrites;

import com.ae2udmigrator.Targets;

/**
 * AE Network Visualization Tool's tool is AE2UD's network visualiser. It keeps what it was bound to and what it
 * drew; its colours and the rest were the tool's own and are left behind.
 */
public final class NetVisualiserMigration {

    private static final String TOOL = "appliedenergistics2:network_visualiser";
    private static final long UNBOUND = new BlockPos(0, -1, 0).toLong();
    private static final byte INTERNAL = 6;

    private NetVisualiserMigration() {
    }

    public static void register() {
        if (Targets.item("aenetvistool:net_visualizer", TOOL)) {
            LegacyRewrites.addStackRewriter("aenetvistool:net_visualizer", NetVisualiserMigration::rewrite);
        }
    }

    private static void rewrite(final NBTTagCompound stack) {
        final NBTTagCompound old = stack.getCompoundTag("tag");
        stack.removeTag("tag");
        if (!old.hasKey("uuid")) {
            return;
        }
        final NBTTagCompound tag = new NBTTagCompound();
        if (old.hasKey("bound_pos", Constants.NBT.TAG_LONG) && old.getLong("bound_pos") != UNBOUND) {
            tag.setLong("visPos", old.getLong("bound_pos"));
            tag.setInteger("visDim", old.getInteger("bound_dim"));
            // The old tool bound to a block, not to one of its sides.
            tag.setByte("visSide", INTERNAL);
        }
        final byte mode = mode(old.getBoolean("enable_cable_connections"),
                old.getBoolean("enable_cable_nodes") || old.getBoolean("enable_machine_nodes"),
                old.getBoolean("enable_chanel_labels"));
        if (mode != 0) {
            tag.setByte("visMode", mode);
        }
        if (!tag.isEmpty()) {
            stack.setTag("tag", tag);
        }
    }

    /**
     * AE2UD's modes: everything, without the channel numbers, the nodes alone, the links alone.
     */
    private static byte mode(final boolean links, final boolean nodes, final boolean labels) {
        if (!links && nodes) {
            return 2;
        }
        if (links && !nodes) {
            return 3;
        }
        if (links && !labels) {
            return 1;
        }
        return 0;
    }
}
