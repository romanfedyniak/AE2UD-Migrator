package com.ae2udmigrator.ae2fc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import appeng.api.migration.LegacyRewrites;
import appeng.api.migration.LegacyStacks;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.ItemStackHelper;
import appeng.helpers.PatternHelper;

import com.ae2udmigrator.Stacks;

/**
 * AE2FC's own patterns. Its processing patterns kept AE stacks, fluids as drops; its crafting pattern with fluid
 * containers is AE2UD's crafting pattern that substitutes fluids.
 */
final class Ae2fcPatterns {

    private static final String PATTERN = Ae2fcMigration.AE + "encoded_pattern";

    private Ae2fcPatterns() {
    }

    static void register() {
        LegacyRewrites.addStackRewriter("ae2fc:dense_encoded_pattern", Ae2fcPatterns::processing);
        LegacyRewrites.addStackRewriter("ae2fc:large_item_encoded_pattern", Ae2fcPatterns::processing);
        LegacyRewrites.addStackRewriter("ae2fc:dense_craft_encoded_pattern", stack -> {
            stack.setString("id", PATTERN);
            stack.getCompoundTag("tag").setBoolean("substitutefluids", true);
            LegacyRewrites.forwardStack(stack);
        });
    }

    private static void processing(final NBTTagCompound stack) {
        final NBTTagCompound tag = stack.getCompoundTag("tag");
        final List<GenericStack> in = read(tag, "Inputs", "in");
        final List<GenericStack> out = read(tag, "Outputs", "out");
        if (in.isEmpty() || out.isEmpty()) {
            LegacyRewrites.log().note("pattern with nothing encoded that can be read; it is a blank pattern now: "
                    + stack);
            blank(stack);
            return;
        }
        if (in.size() > PatternHelper.PROCESSING_INPUT_LIMIT || out.size() > PatternHelper.PROCESSING_OUTPUT_LIMIT) {
            LegacyRewrites.log().note("pattern with " + in.size() + " inputs and " + out.size()
                    + " outputs, which an AE2UD pattern cannot hold; it is a blank pattern now: " + stack);
            blank(stack);
            return;
        }

        final NBTTagCompound encoded = new NBTTagCompound();
        encoded.setTag("in", write(in));
        encoded.setTag("out", write(out));
        encoded.setBoolean("crafting", false);
        encoded.setBoolean("substitute", false);
        encoded.setBoolean("substitutefluids", false);
        if (tag.hasKey("encoderName", Constants.NBT.TAG_STRING)) {
            encoded.setString("author", tag.getString("encoderName"));
        }
        stack.setTag("tag", encoded);
        stack.setString("id", PATTERN);
        stack.setShort("Damage", (short) 0);
    }

    private static void blank(final NBTTagCompound stack) {
        final int count = Math.max(1, Stacks.count(stack));
        Stacks.clear(stack);
        Stacks.become(stack, Ae2fcMigration.AE + "material", 52);
        stack.setByte("Count", (byte) count);
    }

    /**
     * The AE stack list, or, in a pattern that has none, the item stacks. The same key twice is one entry, so
     * that an ultimate encoder's pattern of more inputs than AE2UD's grid fits when it can.
     */
    private static List<GenericStack> read(final NBTTagCompound tag, final String aeKey, final String itemKey) {
        final boolean ae = tag.hasKey(aeKey, Constants.NBT.TAG_LIST);
        final NBTTagList saved = tag.getTagList(ae ? aeKey : itemKey, Constants.NBT.TAG_COMPOUND);
        final Map<AEKey, Long> merged = new LinkedHashMap<>();
        for (int i = 0; i < saved.tagCount(); i++) {
            final NBTTagCompound entry = saved.getCompoundTagAt(i).copy();
            if (Stacks.isEmpty(entry) && !entry.hasKey("Cnt")) {
                continue;
            }
            if (!entry.hasKey("Cnt")) {
                entry.setLong("Cnt", Stacks.count(entry));
            }
            final GenericStack read = LegacyStacks.read(entry);
            if (read == null || read.amount() <= 0) {
                LegacyRewrites.log().lost("pattern entry " + saved.getCompoundTagAt(i));
                continue;
            }
            merged.merge(read.what(), read.amount(), Long::sum);
        }
        final List<GenericStack> stacks = new ArrayList<>(merged.size());
        merged.forEach((what, amount) -> stacks.add(new GenericStack(what, amount)));
        return stacks;
    }

    private static NBTTagList write(final List<GenericStack> stacks) {
        final NBTTagList list = new NBTTagList();
        for (final GenericStack stack : stacks) {
            final NBTTagCompound entry = new NBTTagCompound();
            if (stack.what() instanceof AEItemKey && stack.amount() <= Integer.MAX_VALUE) {
                final ItemStack item = ((AEItemKey) stack.what()).toStack((int) stack.amount());
                ItemStackHelper.stackWriteToNBT(item, entry);
            } else {
                GenericStack.wrapInItemStack(stack).writeToNBT(entry);
            }
            list.appendTag(entry);
        }
        return list;
    }
}
