package com.ae2udmigrator.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

import appeng.api.migration.LegacyRewrites;

import com.ae2udmigrator.Stacks;
import com.ae2udmigrator.Targets;

/**
 * AE Wireless Channel linked one output bus to any number of input buses. In ae2stuff an output bus is a
 * wireless hub, and each input bus a wireless connector paired with it.
 */
public final class WirelessChannelMigration {

    private static final String HUB = "ae2stuff:wireless_hub";
    private static final String CONNECTOR = "ae2stuff:wireless";
    private static final String KIT = "ae2stuff:wireless_kit";
    private static final String OLD = "aewirelesschannel:";
    private static final byte TRANSPARENT = 16;
    private static final int HUB_CONNECTIONS = 32;

    private WirelessChannelMigration() {
    }

    public static void register() {
        for (final String kind : new String[] { "", "dense_" }) {
            bus("block_" + kind + "wireless_output_bus", "tile_" + kind + "wireless_output_bus", HUB, true);
            bus("block_" + kind + "wireless_input_bus", "tile_" + kind + "wireless_input_bus", CONNECTOR, false);
        }
        if (Targets.item(OLD + "item_wireless_key_card", KIT)) {
            LegacyRewrites.addStackRewriter(OLD + "item_wireless_key_card", WirelessChannelMigration::keyCard);
        }
    }

    private static void bus(final String block, final String tile, final String target, final boolean hub) {
        // The old meta held the bus's facing; ae2stuff's blocks have only meta 0, and read any other as air.
        if (!Targets.block(OLD + block, target, 0)) {
            return;
        }
        LegacyRewrites.addStackRewriter(OLD + block, stack -> {
            final NBTTagCompound tag = stack.getCompoundTag("tag");
            if (tag.getBoolean("connect") || !tag.getCompoundTag("connectionInfoTag").isEmpty()) {
                LegacyRewrites.log().note("wireless bus taken down with its link, which ae2stuff does not keep: "
                        + stack);
            }
            stack.removeTag("tag");
        });
        LegacyRewrites.addTileRewriter(OLD + tile, saved -> hub ? hub(saved) : connector(saved));
    }

    private static NBTTagCompound hub(final NBTTagCompound saved) {
        final NBTTagCompound tile = base(saved, HUB);
        final NBTTagCompound links = saved.getCompoundTag("connectionInfoTag");
        final NBTTagList connectors = new NBTTagList();
        for (final String key : links.getKeySet()) {
            final NBTTagCompound link = links.getCompoundTag(key);
            connectors.appendTag(NBTUtil.createPosTag(new BlockPos(link.getInteger("inputBusX"),
                    link.getInteger("inputBusY"), link.getInteger("inputBusZ"))));
        }
        tile.setTag("connectors", connectors);
        LegacyRewrites.log().note("wireless output bus" + Stacks.at(saved) + " is a wireless hub, joined on every"
                + " side, with " + connectors.tagCount() + " connectors");
        if (connectors.tagCount() > HUB_CONNECTIONS) {
            LegacyRewrites.log().note("wireless hub" + Stacks.at(saved) + " keeps its " + connectors.tagCount()
                    + " connectors, more than ae2stuff's default of " + HUB_CONNECTIONS + "; it takes no new ones");
        }
        return tile;
    }

    private static NBTTagCompound connector(final NBTTagCompound saved) {
        final NBTTagCompound tile = base(saved, CONNECTOR);
        final BlockPos self = new BlockPos(saved.getInteger("x"), saved.getInteger("y"), saved.getInteger("z"));
        if (saved.getBoolean("connect")) {
            final BlockPos target = new BlockPos(saved.getInteger("outputBusX"), saved.getInteger("outputBusY"),
                    saved.getInteger("outputBusZ"));
            tile.setTag("target", NBTUtil.createPosTag(target));
            final double distance = Math.sqrt(self.distanceSq(target));
            LegacyRewrites.log().note(String.format("wireless link %d %d %d to %d %d %d: %.1f blocks, %.1f AE/t"
                    + " at each end at ae2stuff's default power settings", self.getX(), self.getY(), self.getZ(),
                    target.getX(), target.getY(), target.getZ(), distance,
                    1 + 0.1 * distance * Math.log(distance * distance + 3)));
        }
        LegacyRewrites.log().note("wireless input bus" + Stacks.at(saved) + " is a wireless connector, joined on"
                + " every side");
        return tile;
    }

    /**
     * What both ends keep: the grid node, and the colour, which ae2stuff reads as white when it is missing.
     */
    private static NBTTagCompound base(final NBTTagCompound saved, final String id) {
        final NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", id);
        for (final String key : new String[] { "x", "y", "z" }) {
            tile.setInteger(key, saved.getInteger(key));
        }
        tile.setTag("proxy", saved.getCompoundTag("aeProxy").copy());
        tile.setByte("color", TRANSPARENT);
        if (saved.getBoolean("wirelessChunkLoadingEnabled")) {
            LegacyRewrites.log().note("wireless bus" + Stacks.at(saved) + " kept its chunk loaded; ae2stuff does not");
        }
        return tile;
    }

    /**
     * A card bound to an output bus is a kit bound to the hub that stands there now.
     */
    private static void keyCard(final NBTTagCompound stack) {
        final NBTTagCompound bound = stack.getCompoundTag("tag").getCompoundTag("outputBusBound");
        stack.removeTag("tag");
        if (bound.isEmpty()) {
            return;
        }
        final NBTTagCompound location = new NBTTagCompound();
        location.setInteger("x", bound.getInteger("boundX"));
        location.setInteger("y", bound.getInteger("boundY"));
        location.setInteger("z", bound.getInteger("boundZ"));
        location.setInteger("dim", bound.getInteger("dimension"));
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("loc", location);
        stack.setTag("tag", tag);
    }
}
