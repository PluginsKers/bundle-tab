package betterbundle.gui;

import betterbundle.util.BundleContentsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BundleRecipeBookPage {
    private static final int PAGE_SIZE = 20;
    private static final WidgetSprites BACK = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_backward"),
            Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    private static final WidgetSprites NEXT = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_forward"),
            Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final Identifier RECIPE_SLOT = Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier BUNDLE_SLOT = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final Identifier HIGHLIGHT = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");

    private final AbstractContainerMenu menu;
    private final List<ItemButton> buttons = new ArrayList<>();
    private EditBox search;
    private ImageButton back;
    private ImageButton previous;
    private ImageButton next;
    private int x;
    private int y;
    private int selectedSlot = -1;
    private int page;
    private int pageCount = 1;
    private int entryCount;
    private String lastQuery = "";

    private record Entry(Slot bundle, int itemIndex, ItemStack stack) {}

    public BundleRecipeBookPage(AbstractContainerMenu menu) {
        this.menu = menu;
        for (int i = 0; i < PAGE_SIZE; i++) buttons.add(new ItemButton());
    }

    public void init(int x, int y, EditBox search) {
        this.x = x;
        this.y = y;
        this.search = search;
        for (int i = 0; i < PAGE_SIZE; i++) buttons.get(i).setPosition(x + 11 + i % 5 * 25, y + 31 + i / 5 * 25);
        back = new ImageButton(x + 118, y + 12, 12, 17, BACK, button -> showBundles(), Component.translatable("better-bundle.back"));
        back.setTooltip(Tooltip.create(back.getMessage()));
        previous = new ImageButton(x + 38, y + 137, 12, 17, BACK, button -> { page--; refresh(); },
                Component.translatable("gui.recipebook.previous_page"));
        previous.setTooltip(Tooltip.create(previous.getMessage()));
        next = new ImageButton(x + 93, y + 137, 12, 17, NEXT, button -> { page++; refresh(); },
                Component.translatable("gui.recipebook.next_page"));
        next.setTooltip(Tooltip.create(next.getMessage()));
        refresh();
    }

    public Slot selectedBundle() {
        if (selectedSlot < 0) return null;
        Slot slot = menu.getSlot(selectedSlot);
        if (BundleContentsHelper.isBundle(slot.getItem())) return slot;
        showBundles();
        return null;
    }

    public void showBundles() {
        selectedSlot = -1;
        page = 0;
        search.setValue("");
        search.setFocused(false);
    }

    private List<Entry> entries() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.containerMenu != menu) return List.of();
        List<Entry> entries = new ArrayList<>();
        Slot selected = selectedBundle();
        if (selected == null) {
            for (Slot slot : BundleContentsHelper.bundleSlots(client.player)) entries.add(new Entry(slot, -1, slot.getItem().copy()));
        } else {
            var items = BundleContentsHelper.contents(selected.getItem()).itemCopies().toList();
            for (int i = 0; i < items.size(); i++) entries.add(new Entry(selected, i, items.get(i)));
        }
        return entries.stream().filter(entry -> BundleContentsHelper.matchesSearch(entry.stack(), search.getValue())).toList();
    }

    private void refresh() {
        List<Entry> entries = entries();
        if (!lastQuery.equals(search.getValue())) {
            page = 0;
            lastQuery = search.getValue();
        }
        entryCount = entries.size();
        pageCount = Math.max(1, (entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.clamp(page, 0, pageCount - 1);
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemButton button = buttons.get(i);
            int index = page * PAGE_SIZE + i;
            button.entry = index < entryCount ? entries.get(index) : null;
            button.visible = button.entry != null;
            if (button.entry != null) button.setMessage(button.entry.stack().getHoverName());
        }
        back.visible = selectedSlot >= 0;
        previous.visible = page > 0;
        next.visible = page + 1 < pageCount;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + 147 && mouseY >= y && mouseY < y + 166;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        refresh();
        if (back.mouseClicked(event, doubleClick) || previous.mouseClicked(event, doubleClick) || next.mouseClicked(event, doubleClick)) return true;
        for (ItemButton button : buttons) {
            if (button.mouseClicked(event, doubleClick)) return true;
        }
        if (contains(event.x(), event.y()) && event.y() >= y + 31) {
            Slot selected = selectedBundle();
            if (selected != null && !menu.getCarried().isEmpty() && isPrimaryOrSecondary(event.button())) {
                BundlePanelInteraction.clickSlot(selected, event.button() == InputConstants.MOUSE_BUTTON_LEFT ? 0 : 1);
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || selectedBundle() != null || menu.getCarried().isEmpty()
                || !contains(event.x(), event.y()) || event.y() < y + 31) return false;
        refresh();
        for (ItemButton button : buttons) {
            if (button.visible && button.isMouseOver(event.x(), event.y())) {
                BundlePanelInteraction.clickSlot(button.entry.bundle(), 0);
                return true;
            }
        }
        if (pageCount > 1 && event.y() >= y + 131) return false;
        entries().stream().map(Entry::bundle)
                .filter(slot -> BundleContentsHelper.canFitItem(slot.getItem(), menu.getCarried()))
                .findFirst().ifPresent(slot -> BundlePanelInteraction.clickSlot(slot, 0));
        return true;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        for (ItemButton button : buttons) button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        back.extractRenderState(graphics, mouseX, mouseY, partialTick);
        previous.extractRenderState(graphics, mouseX, mouseY, partialTick);
        next.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var font = Minecraft.getInstance().font;
        if (entryCount == 0 && (selectedSlot < 0 || !search.getValue().isEmpty())) {
            String key = !search.getValue().isEmpty() ? "better-bundle.no_results"
                    : "better-bundle.no_bundles";
            graphics.textWithWordWrap(font, Component.translatable(key), x + 15, y + 55, 117, 0xFFFFFFFF);
        }
        if (pageCount == 1) return;
        Component footer = Component.translatable("gui.recipebook.page", page + 1, pageCount);
        String label = font.plainSubstrByWidth(footer.getString(), 125);
        graphics.centeredText(font, label, x + 73, y + 141, 0xFFFFFFFF);
    }

    public List<NarratableEntry> narratables() {
        List<NarratableEntry> result = new ArrayList<>();
        for (ItemButton button : buttons) if (button.isActive()) result.add(button);
        if (back.isActive()) result.add(back);
        if (previous.isActive()) result.add(previous);
        if (next.isActive()) result.add(next);
        return result;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    private final class ItemButton extends AbstractWidget {
        private Entry entry;

        private ItemButton() {
            super(0, 0, 25, 25, Component.empty());
        }

        @Override
        protected boolean isValidClickButton(MouseButtonInfo button) {
            return isPrimaryOrSecondary(button.button());
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (!menu.getCarried().isEmpty()) {
                BundlePanelInteraction.clickSlot(entry.bundle(), event.button() == InputConstants.MOUSE_BUTTON_LEFT ? 0 : 1);
            } else if (entry.itemIndex() >= 0) {
                BundlePanelInteraction.takeItem(entry.bundle(), entry.itemIndex(), event.hasShiftDown());
            } else if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                selectedSlot = entry.bundle().index;
                page = 0;
                search.setValue("");
            } else {
                BundlePanelInteraction.clickSlot(entry.bundle(), 1);
            }
            refresh();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean bundleList = entry.itemIndex() < 0;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bundleList ? RECIPE_SLOT : BUNDLE_SLOT,
                    getX(), getY(), bundleList ? 25 : 24, bundleList ? 25 : 24);
            graphics.item(entry.stack(), getX() + 4, getY() + 4);
            graphics.itemDecorations(Minecraft.getInstance().font, entry.stack(), getX() + 4, getY() + 4);
            if (isHoveredOrFocused()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HIGHLIGHT, getX(), getY(), 24, 24);
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, entry.stack(), mouseX, mouseY);
            }
            handleCursor(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
