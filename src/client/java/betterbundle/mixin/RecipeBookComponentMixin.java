package betterbundle.mixin;

import betterbundle.gui.BundleRecipeBookAccess;
import betterbundle.gui.BundleRecipeBookPage;
import betterbundle.gui.BundleTabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin implements BundleRecipeBookAccess {
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;
    @Shadow private RecipeBookTabButton selectedTab;
    @Shadow @Final protected RecipeBookMenu menu;
    @Shadow protected Minecraft minecraft;
    @Shadow private EditBox searchBox;
    @Shadow protected CycleButton<Boolean> filterButton;
    @Shadow private RecipeDisplayId lastRecipe;
    @Shadow private RecipeCollection lastRecipeCollection;
    @Shadow public abstract boolean isVisible();
    @Shadow private int getXOrigin() { throw new AssertionError(); }
    @Shadow private int getYOrigin() { throw new AssertionError(); }
    @Shadow private void onTabButtonPress(Button button) { throw new AssertionError(); }
    @Unique private BundleRecipeBookPage betterbundle$page;

    @Inject(method = "initVisuals", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;selectedTab:Lnet/minecraft/client/gui/screens/recipebook/RecipeBookTabButton;",
            opcode = Opcodes.GETFIELD, ordinal = 0))
    private void addBundleTab(CallbackInfo ci) {
        tabButtons.add(new BundleTabButton(this::onTabButtonPress));
        if (betterbundle$page == null) betterbundle$page = new BundleRecipeBookPage(menu);
        betterbundle$page.init(getXOrigin(), getYOrigin(), searchBox);
    }

    @Inject(method = "initVisuals", at = @At("HEAD"))
    private void releaseOldSearch(CallbackInfo ci) {
        if (searchBox != null) searchBox.setFocused(false);
    }

    @Inject(method = "initVisuals", at = @At("TAIL"))
    private void updateFilterVisibility(CallbackInfo ci) {
        filterButton.visible = !(selectedTab instanceof BundleTabButton);
    }

    @Inject(method = "onTabButtonPress", at = @At("TAIL"))
    private void changePage(Button button, CallbackInfo ci) {
        filterButton.visible = !(selectedTab instanceof BundleTabButton);
        if (selectedTab instanceof BundleTabButton) {
            betterbundle$page.showBundles();
            lastRecipe = null;
            lastRecipeCollection = null;
        }
    }

    @Override
    public BundleRecipeBookPage betterbundle$getPage() {
        return isVisible() && selectedTab instanceof BundleTabButton ? betterbundle$page : null;
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookPage;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIF)V"))
    private void renderBundlePage(RecipeBookPage page, GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        var bundlePage = betterbundle$getPage();
        if (bundlePage != null) bundlePage.extractRenderState(graphics, mouseX, mouseY, partialTick);
        else page.extractRenderState(graphics, x, y, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void clickBundlePage(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        var page = betterbundle$getPage();
        if (page != null && !minecraft.player.isSpectator() && page.mouseClicked(event, doubleClick)) {
            searchBox.setFocused(false);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void protectBookClicks(double mouseX, double mouseY, int left, int top, int width, int height, CallbackInfoReturnable<Boolean> cir) {
        var page = betterbundle$getPage();
        if (page != null && page.contains(mouseX, mouseY)) cir.setReturnValue(false);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void suppressRecipeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Slot hovered, CallbackInfo ci) {
        if (betterbundle$getPage() != null) ci.cancel();
    }

    @Inject(method = "extractGhostRecipe", at = @At("HEAD"), cancellable = true)
    private void suppressGhostRecipe(GuiGraphicsExtractor graphics, boolean bigResult, CallbackInfo ci) {
        if (betterbundle$getPage() != null) ci.cancel();
    }

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void releaseSearch(boolean visible, CallbackInfo ci) {
        if (!visible && searchBox != null) searchBox.setFocused(false);
    }

    @Inject(method = "updateNarration", at = @At("HEAD"), cancellable = true)
    private void narrateBundlePage(NarrationElementOutput output, CallbackInfo ci) {
        var page = betterbundle$getPage();
        if (page == null) return;
        var entries = page.narratables();
        entries.add(searchBox);
        entries.addAll(tabButtons);
        var result = Screen.findNarratableWidget(entries, null);
        if (result != null) result.entry().updateNarration(output.nest());
        ci.cancel();
    }
}
