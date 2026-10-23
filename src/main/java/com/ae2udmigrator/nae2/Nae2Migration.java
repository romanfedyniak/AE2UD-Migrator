package com.ae2udmigrator.nae2;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import appeng.api.migration.LegacyRewrites;
import appeng.api.migration.LegacyStacks;

import com.ae2udmigrator.Stacks;
import com.ae2udmigrator.Targets;

/**
 * NAE2, from its last release and the dense beam former and portable cells of later builds: AE2UD took most of
 * it in, and its beam formers went to Beam Formers.
 */
public final class Nae2Migration {

    private static final String AE = "appliedenergistics2:";
    private static final String[] SIZES = { "256k", "1024k", "4096k", "16384k" };

    /**
     * {@code nae2:material} by metadata: the void cell component, then item, fluid and gas cell components,
     * which are one component each in AE2UD.
     */
    private static final int[] MATERIALS = { -1, 67, 68, 69, 70, 67, 68, 69, 70, 67, 68, 69, 70 };

    /**
     * {@code nae2:upgrade} by metadata: hyper-acceleration, auto-complete, which is AE2UD's fake crafting card,
     * and the GregTech circuit card.
     */
    private static final int[] UPGRADES = { -1, 63, -1 };

    private static final String SMALL_BEAM_FORMER = "beamformers:small_beam_former";

    private Nae2Migration() {
    }

    public static void register() {
        for (final String size : SIZES) {
            Targets.block("nae2:storage_crafting_" + size, AE + "crafting_storage_" + size);
            cell("nae2:storage_cell_" + size, AE + "storage_cell_" + size);
            cell("nae2:storage_cell_fluid_" + size, AE + "fluid_storage_cell_" + size);
            cell("nae2:storage_cell_gas_" + size, "mekeng:gas_cell_" + size);
        }
        for (final String multiplier : new String[] { "4x", "16x", "64x" }) {
            Targets.block("nae2:coprocessor_" + multiplier, AE + "crafting_accelerator_" + multiplier);
        }
        LegacyRewrites.addTileRewriter("nae2:crafting_storage", Nae2Migration::craftingTile);

        if (Targets.block("nae2:exposer", AE + "exposer")) {
            LegacyRewrites.addTileRewriter("nae2:exposer", tile -> {
                tile.setString("id", AE + "exposer");
                return tile;
            });
        }

        // Its items wait in a chest beside where it stood.
        LegacyRewrites.remove("nae2:reconstruction_chamber");
        LegacyRewrites.addTileRewriter("nae2:reconstruction_chamber", tile -> {
            Stacks.spillSlots(tile.getCompoundTag("inv"));
            tile.removeTag("inv");
            return tile;
        });

        if (Targets.block("nae2:dense_beam_former", "beamformers:dense_beam_former")) {
            LegacyRewrites.addTileRewriter("nae2:dense_beam_former", tile -> {
                tile.setString("id", "beamformers:dense_beam_former");
                tile.removeTag("beamLength");
                return tile;
            });
        }

        for (final String gone : new String[] { "nae2:storage_cell_void", "nae2:fluid_storage_cell_void",
                "nae2:gas_storage_cell_void", "nae2:virtual_pattern" }) {
            LegacyRewrites.remove(gone);
        }
        cell("nae2:improved_portable_cell", AE + "portable_cell_1024k");
        cell("nae2:advanced_portable_cell", AE + "portable_cell_4096k");
        cell("nae2:perfect_portable_cell", AE + "portable_cell_16384k");
        Targets.item("nae2:pattern_multiplier", AE + "pattern_multiplier");

        LegacyRewrites.addStackRewriter("nae2:material", stack -> byMeta(stack, AE + "material", MATERIALS));
        LegacyRewrites.addStackRewriter("nae2:upgrade", stack -> byMeta(stack, AE + "material", UPGRADES));
        LegacyRewrites.addStackRewriter("nae2:part", Nae2Migration::part);
        LegacyRewrites.addPartRewriter("nae2:part", Nae2Migration::beamFormer);

        // A void cell in a condenser, which NAE2 added; the cell itself is gone.
        LegacyRewrites.addTileRewriter(AE + "condenser", tile -> {
            final NBTTagList cells = tile.getCompoundTag("voidCellInv").getTagList("Items",
                    Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < cells.tagCount(); i++) {
                LegacyRewrites.log().lost("condenser" + Stacks.at(tile) + ": " + cells.getCompoundTagAt(i));
            }
            tile.removeTag("voidCellInv");
            return tile;
        });
    }

    private static void cell(final String oldId, final String newId) {
        if (Targets.item(oldId, newId)) {
            LegacyStacks.addCell(oldId);
        }
    }

    private static void byMeta(final NBTTagCompound stack, final String id, final int[] metas) {
        final int meta = stack.getShort("Damage");
        if (meta < 0 || meta >= metas.length || metas[meta] < 0) {
            Stacks.lose(stack);
            return;
        }
        Stacks.become(stack, id, metas[meta]);
    }

    private static void part(final NBTTagCompound stack) {
        switch (stack.getShort("Damage")) {
            case 0:
                if (Targets.hasItem(SMALL_BEAM_FORMER)) {
                    Stacks.become(stack, SMALL_BEAM_FORMER, 0);
                } else {
                    Stacks.lose(stack);
                }
                break;
            case 1:
                Stacks.become(stack, AE + "part", 471);
                break;
            case 2:
                Stacks.become(stack, AE + "part", 540);
                break;
            default:
                Stacks.lose(stack);
                break;
        }
    }

    /**
     * NAE2 kept its beam's flags inside the part's grid node; Beam Formers keeps whether the beam is hidden
     * beside it, and measures the length again.
     */
    private static void beamFormer(final NBTTagCompound item, final NBTTagCompound part) {
        if (item.getShort("Damage") != 0) {
            return;
        }
        final NBTTagCompound node = part.getCompoundTag("part");
        if (node.getBoolean("hideBeam")) {
            part.setBoolean("hideBeam", true);
        }
        node.removeTag("hideBeam");
        node.removeTag("beamLength");
    }

    /**
     * NAE2 gave its storage blocks and co-processors one tile entity; AE2UD's storage and accelerators each
     * have their own, and which one a tile is follows from the block it stands in.
     */
    private static NBTTagCompound craftingTile(final NBTTagCompound tile) {
        final Block block = LegacyRewrites.blockAt(tile);
        final ResourceLocation name = block == null ? null : block.getRegistryName();
        final boolean accelerator = name != null && name.getPath().startsWith("crafting_accelerator");
        tile.setString("id", AE + (accelerator ? "crafting_unit" : "crafting_storage"));
        return LegacyRewrites.forwardTile(tile);
    }
}
