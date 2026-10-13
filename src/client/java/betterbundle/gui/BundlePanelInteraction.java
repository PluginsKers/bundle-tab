package betterbundle.gui;

import betterbundle.util.BundleContentsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

public final class BundlePanelInteraction {
    private BundlePanelInteraction() {}

    public static void clickSlot(Slot slot, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.player.isSpectator()) return;
        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot.index, button,
                ContainerInput.PICKUP, client.player);
    }

    public static void takeItem(Slot bundle, int itemIndex, boolean quickMove) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || client.player.isSpectator()
                || !client.player.containerMenu.getCarried().isEmpty() || !bundle.mayPickup(client.player)) return;
        var contents = BundleContentsHelper.contents(bundle.getItem());
        if (itemIndex < 0 || itemIndex >= contents.size()) return;
        if (BundleItem.getSelectedItemIndex(bundle.getItem()) != itemIndex) {
            client.player.containerMenu.setSelectedBundleItemIndex(bundle.index, itemIndex);
            client.getConnection().send(new ServerboundSelectBundleItemPacket(bundle.index, itemIndex));
        }
        clickSlot(bundle, 1);
        if (!quickMove) return;

        // Use normal inventory clicks so prediction, stack limits and server validation stay vanilla.
        for (boolean occupied : new boolean[]{true, false}) {
            for (Slot slot : client.player.containerMenu.slots) {
                ItemStack carried = client.player.containerMenu.getCarried();
                if (carried.isEmpty()) return;
                if (slot.container != client.player.getInventory() || slot.getContainerSlot() >= 36
                        || slot.hasItem() != occupied || !slot.mayPlace(carried)) continue;
                if (occupied && (!ItemStack.isSameItemSameComponents(slot.getItem(), carried)
                        || slot.getItem().getCount() >= slot.getMaxStackSize(carried))) continue;
                clickSlot(slot, 0);
            }
        }
        if (!client.player.containerMenu.getCarried().isEmpty()) clickSlot(bundle, 0);
    }

    public static boolean handleSpaceClick(Slot source) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.isSpectator() || source == null || !source.hasItem()
                || !source.mayPickup(client.player) || !client.player.containerMenu.getCarried().isEmpty()
                || !InputConstants.isKeyDown(InputConstants.KEY_SPACE)) return false;
        if (BundleContentsHelper.isBundle(source.getItem()) && !BundleContentsHelper.contents(source.getItem()).isEmpty()) return false;
        Slot target = null;
        if (client.gui.screen() instanceof AbstractRecipeBookScreen<?> screen) {
            var page = ((BundleRecipeBookAccess) screen.recipeBookComponent).betterbundle$getPage();
            if (page != null) target = page.selectedBundle();
        }
        if (target == null) {
            target = BundleContentsHelper.bundleSlots(client.player).stream()
                    .filter(slot -> slot != source && BundleContentsHelper.canFitItem(slot.getItem(), source.getItem()))
                    .findFirst().orElse(null);
        }
        if (target == null || target == source || !BundleContentsHelper.canFitItem(target.getItem(), source.getItem())) return false;
        clickSlot(source, 0);
        clickSlot(target, 0);
        if (!client.player.containerMenu.getCarried().isEmpty()) clickSlot(source, 0);
        return true;
    }
}
