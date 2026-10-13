package betterbundle.gui;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;

import java.util.Optional;

public final class BundleTabButton extends RecipeBookTabButton {
    private static final ExtendedRecipeBookCategory CATEGORY = new ExtendedRecipeBookCategory() {};

    public BundleTabButton(Button.OnPress onPress) {
        super(0, 0, new RecipeBookComponent.TabInfo(new ItemStack(Items.BUNDLE), Optional.empty(), CATEGORY), onPress);
        setMessage(Component.translatable("better-bundle.tab"));
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    public boolean updateVisibility(ClientRecipeBook book) {
        return visible = true;
    }
}
