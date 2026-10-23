package com.ae2udmigrator;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import appeng.api.migration.LegacyRewrites;

/**
 * Where old content goes, when that is there: a target from an optional mod, or one AE2UD's config turned off,
 * may not be. Content whose target is missing is gone, as content with no equivalent is.
 */
public final class Targets {

    private Targets() {
    }

    public static boolean hasBlock(final String id) {
        return ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(id));
    }

    public static boolean hasItem(final String id) {
        return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id));
    }

    /**
     * A block and its item renamed, metadata and tile entity kept.
     */
    public static boolean block(final String oldId, final String newId) {
        if (!hasBlock(newId)) {
            gone(oldId, newId);
            return false;
        }
        LegacyRewrites.remapBlock(oldId, newId);
        return true;
    }

    /**
     * A block and its item that are one metadata of another block now.
     */
    public static boolean block(final String oldId, final String newId, final int meta) {
        if (!hasBlock(newId)) {
            gone(oldId, newId);
            return false;
        }
        LegacyRewrites.remapBlock(oldId, newId, meta);
        LegacyRewrites.addStackRewriter(oldId, stack -> stack.setShort("Damage", (short) meta));
        return true;
    }

    public static boolean item(final String oldId, final String newId) {
        if (!hasItem(newId)) {
            gone(oldId, newId);
            return false;
        }
        LegacyRewrites.remapItem(oldId, newId);
        return true;
    }

    public static void gone(final String oldId, final String missing) {
        LegacyRewrites.remove(oldId);
        AE2UDMigrator.LOG.info("{} has nothing to become in this game: {} is not registered", oldId, missing);
    }
}
