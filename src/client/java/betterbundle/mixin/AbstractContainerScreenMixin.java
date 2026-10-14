package betterbundle.mixin;

import betterbundle.gui.BundlePanelInteraction;
import betterbundle.gui.BundleRecipeBookAccess;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Unique private long betterbundle$dragStart;
    @Unique private int betterbundle$lastSlot = -1;

    @Inject(method = {"init()V", "removed()V"}, at = @At("HEAD"))
    private void resetInput(CallbackInfo ci) {
        betterbundle$dragStart = 0;
        betterbundle$lastSlot = -1;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !InputConstants.isKeyDown(InputConstants.KEY_SPACE)) return;
        Slot slot = ((AbstractContainerScreen<?>) (Object) this).hoveredSlot;
        betterbundle$dragStart = System.currentTimeMillis();
        if (BundlePanelInteraction.handleSpaceClick(slot)) {
            betterbundle$lastSlot = slot.index;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"), cancellable = true)
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        betterbundle$dragStart = 0;
        betterbundle$lastSlot = -1;
        // Let vanilla finish its drag state before handling a drop on the bundle page.
        if ((Object) this instanceof AbstractRecipeBookScreen<?> screen) {
            var page = ((BundleRecipeBookAccess) screen.recipeBookComponent).betterbundle$getPage();
            if (page != null && page.mouseReleased(event)) cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (betterbundle$dragStart == 0 || System.currentTimeMillis() - betterbundle$dragStart < 50
                || !InputConstants.isKeyDown(InputConstants.KEY_SPACE) || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return;
        Slot slot = ((AbstractContainerScreen<?>) (Object) this).hoveredSlot;
        if (slot != null && slot.index != betterbundle$lastSlot && BundlePanelInteraction.handleSpaceClick(slot)) {
            betterbundle$lastSlot = slot.index;
        }
        cir.setReturnValue(true);
    }
}
