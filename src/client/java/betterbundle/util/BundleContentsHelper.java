package betterbundle.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.List;
import java.util.Locale;

public final class BundleContentsHelper {
    private BundleContentsHelper() {}

    public static boolean isBundle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BundleItem;
    }

    public static BundleContents contents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    public static List<Slot> bundleSlots(Player player) {
        return player.containerMenu.slots.stream()
                .filter(slot -> slot.container == player.getInventory() && isBundle(slot.getItem())).toList();
    }

    public static boolean canFitItem(ItemStack bundle, ItemStack item) {
        return isBundle(bundle) && !item.isEmpty() && contents(bundle).asMutable().tryInsert(item.copy()) > 0;
    }

    public static boolean matchesSearch(ItemStack stack, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(q) || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(q)) return true;
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        StringBuilder pinyin = new StringBuilder();
        try {
            for (char c : name.toCharArray()) {
                String[] readings = PinyinHelper.toHanyuPinyinStringArray(c, format);
                if (readings != null && readings.length > 0) pinyin.append(readings[0]);
                else pinyin.append(c);
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            throw new IllegalStateException(e);
        }
        return pinyin.toString().contains(q);
    }
}
