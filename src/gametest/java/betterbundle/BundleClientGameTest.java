package betterbundle;

import betterbundle.gui.BundlePanelInteraction;
import betterbundle.gui.BundleRecipeBookAccess;
import betterbundle.gui.BundleRecipeBookPage;
import betterbundle.gui.BundleTabButton;
import betterbundle.util.BundleContentsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.lwjgl.sdl.SDLKeyboard;

import java.util.List;

public final class BundleClientGameTest implements FabricClientGameTest {
    private static void setLanguage(ClientGameTestContext context, String language) {
        context.runOnClient(client -> {
            client.getLanguageManager().setSelected(language);
            client.options.languageCode = language;
            client.getLanguageManager().onResourceManagerReload(client.getResourceManager());
        });
        context.waitFor(client -> client.gui.overlay() == null);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> { client.options.guiScale().set(2); client.resizeGui(); });
        setLanguage(context, "en_us");
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.getInventory().setItem(0, bundle(Items.DYED_BUNDLE.red(), "建筑材料", List.of(
                        new ItemStackTemplate(Items.STONE, 8), new ItemStackTemplate(Items.DIRT, 4))));
                player.getInventory().setItem(1, bundle(Items.DYED_BUNDLE.blue(), "工具袋", List.of()));
                player.getInventory().setItem(2, bundle(Items.DYED_BUNDLE.white(), "满袋", List.of(new ItemStackTemplate(Items.COBBLESTONE, 64))));
                player.getInventory().setItem(3, bundle(Items.BUNDLE, "杂物", List.of(
                        Items.STONE, Items.DIRT, Items.COBBLESTONE, Items.ANDESITE, Items.DIORITE, Items.GRANITE,
                        Items.DEEPSLATE, Items.TUFF, Items.GRAVEL, Items.SAND, Items.RED_SAND, Items.MOSS_BLOCK,
                        Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.CLAY, Items.BRICKS, Items.OAK_PLANKS,
                        Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS,
                        Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS, Items.BAMBOO_PLANKS,
                        Items.PALE_OAK_PLANKS, Items.CALCITE, Items.OBSIDIAN).stream().map(item -> new ItemStackTemplate(item, 1)).toList()));
                player.getInventory().setItem(9, new ItemStack(Items.STONE, 60));
                player.getInventory().setItem(10, new ItemStack(Items.DIRT, 16));
                player.inventoryMenu.broadcastChanges();
            });
            sync(world);
            context.runOnClient(client -> client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false));
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.runOnClient(client -> {
                check(book(client).isVisible() && page(client) != null && page(client).selectedBundle() == null,
                        "Opening the inventory automatically shows the bundle gallery");
                var tabs = field(book(client), "tabButtons", List.class);
                check(tabs.getFirst() instanceof BundleTabButton
                                && field(book(client), "selectedTab", RecipeBookTabButton.class) == tabs.getFirst(),
                        "The bundle tab is first and selected by default");
                check(!field(book(client), "filterButton", CycleButton.class).visible, "The bundle gallery hides the recipe filter");
            });
            context.takeScreenshot("recipebook-bundles");
            setLanguage(context, "zh_cn");

            clickTab(context, false);
            context.getInput().resizeWindow(640, 480);
            context.runOnClient(client -> check(book(client).isVisible() && page(client) == null,
                    "Resizing preserves a manually selected recipe tab"));
            context.runOnClient(client -> book(client).toggleVisibility());
            context.getInput().resizeWindow(1280, 720);
            context.runOnClient(client -> check(!book(client).isVisible(), "Resizing preserves a manually closed book"));
            context.setScreen(() -> null);
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.runOnClient(client -> check(book(client).isVisible() && page(client) != null
                            && page(client).selectedBundle() == null && search(client).getValue().isEmpty(),
                    "Reopening the inventory returns to the bundle gallery after closing recipes"));

            typeSearch(context, "石😀");
            context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
            context.runOnClient(client -> check(search(client).getValue().equals("石"), "Native search must delete one Unicode codepoint"));
            openBundle(context, "gongju");
            context.runOnClient(client -> check(page(client).selectedBundle().getItem().is(Items.DYED_BUNDLE.blue()), "Pinyin search and empty bundle selection"));
            context.takeScreenshot("recipebook-empty-bundle");

            openBundle(context, "jianzhu");
            context.takeScreenshot("recipebook-bundle-contents");
            clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, 0);
            sync(world);
            checkCursor(context, world, Items.STONE, 8);
            clickCell(context, 19, InputConstants.MOUSE_BUTTON_LEFT, 0);
            sync(world);
            checkCursor(context, world, Items.AIR, 0);
            world.getServer().runOnServer(server -> {
                var inventory = world.getConnection().getServerPlayer().getInventory();
                check(bundleCount(inventory.getItem(0), Items.STONE) == 8, "Insert into the selected red bundle");
                check(BundleContentsHelper.contents(inventory.getItem(1)).isEmpty(), "Do not insert into a different bundle");
            });

            openBundle(context, "jianzhu");
            clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, org.lwjgl.sdl.SDLKeycode.SDL_KMOD_LSHIFT);
            sync(world);
            clickCell(context, 0, InputConstants.MOUSE_BUTTON_RIGHT, org.lwjgl.sdl.SDLKeycode.SDL_KMOD_RSHIFT);
            sync(world);
            checkCursor(context, world, Items.AIR, 0);
            world.getServer().runOnServer(server -> {
                var inventory = world.getConnection().getServerPlayer().getInventory();
                check(inventory.getItem(9).getCount() == 64, "Shift-click merges into an existing stack");
                check(inventory.getItem(11).is(Items.STONE) && inventory.getItem(11).getCount() == 4, "Shift-click moves the remainder to an empty slot");
                check(inventory.getItem(10).getCount() == 20, "Both Shift modifiers transfer the complete selected stack");
                check(BundleContentsHelper.contents(inventory.getItem(0)).isEmpty(), "Both extracted stacks leave the bundle");
            });

            openBundle(context, "gongju");
            spaceClick(context, 10);
            sync(world);
            spaceClick(context, 9);
            sync(world);
            world.getServer().runOnServer(server -> {
                var inventory = world.getConnection().getServerPlayer().getInventory();
                check(bundleCount(inventory.getItem(1), Items.DIRT) == 20 && bundleCount(inventory.getItem(1), Items.STONE) == 44,
                        "Space-click targets the selected bundle and respects its remaining capacity");
                check(inventory.getItem(9).getCount() == 20, "Partial insertion returns excess items to the original slot");
            });
            openBundle(context, "满袋");
            context.runOnClient(client -> BundlePanelInteraction.clickSlot(client.player.inventoryMenu.getSlot(9), 0));
            clickCell(context, 19, InputConstants.MOUSE_BUTTON_LEFT, 0);
            sync(world);
            checkCursor(context, world, Items.STONE, 20);
            context.runOnClient(client -> BundlePanelInteraction.clickSlot(client.player.inventoryMenu.getSlot(9), 0));
            sync(world);

            openBundle(context, "杂物");
            clickBook(context, 99, 144, InputConstants.MOUSE_BUTTON_LEFT, 0);
            context.takeScreenshot("recipebook-bundle-page-two");
            clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, 0);
            sync(world);
            checkCursor(context, world, Items.ACACIA_PLANKS, 1);
            clickCell(context, 19, InputConstants.MOUSE_BUTTON_LEFT, 0);
            sync(world);

            openBundle(context, "gongju");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.getInventory().setItem(35, player.getInventory().getItem(1));
                player.getInventory().setItem(1, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
            });
            sync(world);
            context.runOnClient(client -> check(page(client).selectedBundle() == null, "Server-side movement invalidates the selected slot"));

            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.getInventory().setItem(30, new ItemStack(Items.OAK_PLANKS, 8));
                player.inventoryMenu.broadcastChanges();
            });
            world.getServer().runCommand("recipe give Player0 *");
            sync(world);
            clickTab(context, false);
            context.runOnClient(client -> check(page(client) == null
                    && field(book(client), "filterButton", CycleButton.class).visible, "Vanilla recipe tab and filter are restored"));
            typeSearch(context, "木棍");
            clickBook(context, 123, 19, InputConstants.MOUSE_BUTTON_LEFT, 0);
            context.takeScreenshot("recipebook-vanilla-crafting");
            clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, 0);
            context.waitFor(client -> client.player.inventoryMenu.getSlot(0).getItem().is(Items.STICK));
            sync(world);
            clickTab(context, true);
            context.takeScreenshot("recipebook-with-vanilla-tabs");

            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new CraftingMenu(id, inventory),
                        Component.translatable("container.crafting")));
            });
            context.waitForScreen(CraftingScreen.class);
            clickTab(context, true);
            openBundle(context, "gongju");
            context.takeScreenshot("recipebook-crafting-table");
            context.getInput().resizeWindow(640, 480);
            context.runOnClient(client -> check(page(client) != null && page(client).selectedBundle().getItem().is(Items.DYED_BUNDLE.blue()),
                    "Resize preserves the selected native tab and bundle"));
            context.takeScreenshot("recipebook-narrow-window");
            context.getInput().resizeWindow(1280, 720);
            typeSearch(context, "石");
            context.runOnClient(client -> check(SDLKeyboard.SDL_TextInputActive(client.getWindow().handle()), "Native search enables SDL text input"));
            context.setScreen(() -> null);
            context.runOnClient(client -> check(!SDLKeyboard.SDL_TextInputActive(client.getWindow().handle()), "Closing the screen releases native text input"));

            checkFullInventory(context, world);
            checkGalleryDrop(context, world);
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mousetweaks")) {
                checkVanillaInventoryDrag(context, world);
            }
        }
    }

    private static void checkFullInventory(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.closeContainer();
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(0, bundle(Items.DYED_BUNDLE.red(), "建筑材料", List.of(new ItemStackTemplate(Items.STONE, 8))));
            player.inventoryMenu.broadcastChanges();
        });
        sync(world);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        clickTab(context, true);
        openBundle(context, "jianzhu");
        clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, org.lwjgl.sdl.SDLKeycode.SDL_KMOD_LSHIFT);
        sync(world);
        checkCursor(context, world, Items.AIR, 0);
        world.getServer().runOnServer(server -> check(bundleCount(world.getConnection().getServerPlayer().getInventory().getItem(0), Items.STONE) == 8,
                "A full inventory returns the extracted stack to the original bundle"));
        context.setScreen(() -> null);
    }

    private static void checkGalleryDrop(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
            player.getInventory().setItem(0, bundle(Items.DYED_BUNDLE.red(), "原袋", List.of()));
            player.getInventory().setItem(1, bundle(Items.DYED_BUNDLE.blue(), "目标", List.of()));
            player.getInventory().setItem(2, bundle(Items.DYED_BUNDLE.white(), "满袋", List.of(new ItemStackTemplate(Items.DIRT, 64))));
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 48));
            player.getInventory().setItem(10, new ItemStack(Items.STONE, 32));
            player.inventoryMenu.broadcastChanges();
        });
        sync(world);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        clickTab(context, true);
        typeSearch(context, "目标");
        moveToInventory(context, 9);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveToGalleryCell(context, 0);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.AIR, 0);
        context.runOnClient(client -> check(page(client).selectedBundle() == null, "Dropping keeps the bundle gallery open"));
        world.getServer().runOnServer(server -> {
            var inventory = world.getConnection().getServerPlayer().getInventory();
            check(bundleCount(inventory.getItem(1), Items.STONE) == 48 && BundleContentsHelper.contents(inventory.getItem(0)).isEmpty(),
                    "Filtered gallery drop uses the displayed bundle");
        });

        moveToInventory(context, 10);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveToGalleryCell(context, 0);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.STONE, 16);
        typeSearch(context, "满袋");
        moveToGalleryCell(context, 0);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.STONE, 16);
        typeSearch(context, "原袋");
        moveToGalleryCell(context, 0);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.AIR, 0);
        world.getServer().runOnServer(server -> {
            var inventory = world.getConnection().getServerPlayer().getInventory();
            check(bundleCount(inventory.getItem(0), Items.STONE) == 16 && bundleCount(inventory.getItem(1), Items.STONE) == 64
                    && bundleCount(inventory.getItem(2), Items.DIRT) == 64, "Full bags keep the remainder and all physical bundles in place");
        });

        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.getInventory().setItem(0, bundle(Items.DYED_BUNDLE.red(), "原袋", List.of(new ItemStackTemplate(Items.DIRT, 64))));
            player.getInventory().setItem(1, bundle(Items.DYED_BUNDLE.blue(), "目标", List.of(new ItemStackTemplate(Items.STONE, 60))));
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 8));
            player.getInventory().setItem(11, new ItemStack(Items.SHULKER_BOX));
            player.inventoryMenu.broadcastChanges();
        });
        sync(world);
        typeSearch(context, "");
        moveToInventory(context, 9);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveToGalleryCell(context, 19);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.STONE, 4);
        world.getServer().runOnServer(server -> {
            var inventory = world.getConnection().getServerPlayer().getInventory();
            check(bundleCount(inventory.getItem(0), Items.DIRT) == 64 && bundleCount(inventory.getItem(1), Items.STONE) == 64
                    && bundleCount(inventory.getItem(2), Items.DIRT) == 64, "Blank gallery drop skips full bags and keeps overflow on the cursor");
        });
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.STONE, 4);

        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.getInventory().setItem(2, bundle(Items.DYED_BUNDLE.white(), "满袋", List.of()));
            player.inventoryMenu.broadcastChanges();
        });
        sync(world);
        typeSearch(context, "原袋");
        moveToGalleryCell(context, 19);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.STONE, 4);
        typeSearch(context, "");
        sync(world);
        checkCursor(context, world, Items.STONE, 4);
        moveToGalleryCell(context, 19);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.AIR, 0);
        context.runOnClient(client -> check(page(client).selectedBundle() == null, "Automatic deposit keeps the gallery open"));
        world.getServer().runOnServer(server -> check(
                bundleCount(world.getConnection().getServerPlayer().getInventory().getItem(2), Items.STONE) == 4,
                "Automatic deposit respects the search filter and uses the next bag with space"));
        context.takeScreenshot("recipebook-automatic-deposit");

        moveToInventory(context, 11);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveToGalleryCell(context, 19);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.SHULKER_BOX, 1);
        // A different slot avoids vanilla's double-click collection.
        moveToInventory(context, 12);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        sync(world);
        checkCursor(context, world, Items.AIR, 0);
        context.setScreen(() -> null);
    }

    private static void moveToGalleryCell(ClientGameTestContext context, int index) {
        var position = context.computeOnClient(client -> new double[]{
                (search(client).getX() - 25 + 20 + index % 5 * 25) * client.getWindow().getGuiScale(),
                (search(client).getY() - 13 + 40 + index / 5 * 25) * client.getWindow().getGuiScale()});
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTick();
    }

    private static void checkVanillaInventoryDrag(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
            player.getInventory().setItem(0, bundle(Items.DYED_BUNDLE.red(), "拖拽", List.of(
                    new ItemStackTemplate(Items.STONE, 8), new ItemStackTemplate(Items.DIRT, 4))));
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 16));
            player.getInventory().setItem(10, new ItemStack(Items.STONE, 4));
            player.inventoryMenu.broadcastChanges();
        });
        sync(world);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        clickTab(context, true);
        openBundle(context, "拖拽");
        typeSearch(context, "石头");

        for (int view = 0; view < 3; view++) {
            if (view == 1) clickTab(context, false);
            if (view == 2) context.runOnClient(client -> book(client).toggleVisibility());
            moveToInventory(context, 9);
            context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            moveToInventory(context, 10);
            moveToInventory(context, 0);
            context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            sync(world);
            checkCursor(context, world, Items.STONE, 20);
            world.getServer().runOnServer(server -> check(
                    world.getConnection().getServerPlayer().getInventory().getItem(0).is(Items.DYED_BUNDLE.red()),
                    "Inventory-only collection must not pick up the physical bundle"));
            moveToInventory(context, 11);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            sync(world);
            checkCursor(context, world, Items.AIR, 0);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.getInventory().setItem(11, ItemStack.EMPTY);
                player.getInventory().setItem(9, new ItemStack(Items.STONE, 16));
                player.getInventory().setItem(10, new ItemStack(Items.STONE, 4));
                player.inventoryMenu.broadcastChanges();
            });
            sync(world);
        }
        context.setScreen(() -> null);
    }

    private static void moveToInventory(ClientGameTestContext context, int index) {
        var position = context.computeOnClient(client -> {
            var screen = (AbstractRecipeBookScreen<?>) client.gui.screen();
            var slot = client.player.containerMenu.slots.stream().filter(s -> s.container == client.player.getInventory()
                    && s.getContainerSlot() == index).findFirst().orElseThrow();
            return new double[]{(screen.leftPos + slot.x + 8) * client.getWindow().getGuiScale(),
                    (screen.topPos + slot.y + 8) * client.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTick();
    }

    private static ItemStack bundle(Item item, String name, List<ItemStackTemplate> contents) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(contents));
        return stack;
    }

    private static int bundleCount(ItemStack bundle, Item item) {
        return BundleContentsHelper.contents(bundle).itemCopies().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void sync(TestSingleplayerContext world) {
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
    }

    private static RecipeBookComponent<?> book(Minecraft client) {
        return ((AbstractRecipeBookScreen<?>) client.gui.screen()).recipeBookComponent;
    }

    private static BundleRecipeBookPage page(Minecraft client) {
        return ((BundleRecipeBookAccess) book(client)).betterbundle$getPage();
    }

    private static EditBox search(Minecraft client) {
        return field(book(client), "searchBox", EditBox.class);
    }

    private static <T> T field(RecipeBookComponent<?> book, String name, Class<T> type) {
        try {
            var field = RecipeBookComponent.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(book));
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void clickTab(ClientGameTestContext context, boolean bundle) {
        context.runOnClient(client -> {
            var book = book(client);
            check(book.isVisible(), "Vanilla recipe book must be open");
            for (Object value : field(book, "tabButtons", List.class)) {
                var tab = (RecipeBookTabButton) value;
                if ((tab instanceof BundleTabButton) == bundle && tab.visible) {
                    check(client.gui.screen().mouseClicked(new MouseButtonEvent(tab.getX() + 10, tab.getY() + 10,
                            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false), "Native tab click");
                    return;
                }
            }
            throw new AssertionError("Missing tab");
        });
    }

    private static void typeSearch(ClientGameTestContext context, String text) {
        clickBook(context, 35, 18, InputConstants.MOUSE_BUTTON_LEFT, 0);
        // Fabric's simulated key events currently omit SDL modifier flags.
        context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_A, 'a',
                org.lwjgl.sdl.SDLKeycode.SDL_KMOD_LCTRL)));
        context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
        context.getInput().typeChars(text);
        context.runOnClient(client -> check(search(client).getValue().equals(text),
                "Native search field: expected " + text + ", got " + search(client).getValue()));
    }

    private static void openBundle(ClientGameTestContext context, String query) {
        if (context.computeOnClient(client -> page(client).selectedBundle() != null)) clickBook(context, 124, 20, InputConstants.MOUSE_BUTTON_LEFT, 0);
        typeSearch(context, query);
        clickCell(context, 0, InputConstants.MOUSE_BUTTON_LEFT, 0);
        context.runOnClient(client -> check(page(client).selectedBundle() != null, "Click a matching bundle to enter it"));
    }

    private static void clickCell(ClientGameTestContext context, int index, int button, int modifiers) {
        clickBook(context, 20 + index % 5 * 25, 40 + index / 5 * 25, button, modifiers);
    }

    private static void clickBook(ClientGameTestContext context, int x, int y, int button, int modifiers) {
        context.runOnClient(client -> {
            var search = search(client);
            check(client.gui.screen().mouseClicked(new MouseButtonEvent(search.getX() - 25 + x, search.getY() - 13 + y,
                    new MouseButtonInfo(button, modifiers)), false), "Recipe book click must be consumed");
            client.gui.screen().mouseReleased(new MouseButtonEvent(search.getX() - 25 + x, search.getY() - 13 + y,
                    new MouseButtonInfo(button, modifiers)));
        });
    }

    private static void spaceClick(ClientGameTestContext context, int inventorySlot) {
        context.getInput().holdKey(InputConstants.KEY_SPACE);
        context.runOnClient(client -> {
            var screen = (AbstractRecipeBookScreen<?>) client.gui.screen();
            var slot = client.player.containerMenu.slots.stream().filter(s -> s.container == client.player.getInventory()
                    && s.getContainerSlot() == inventorySlot).findFirst().orElseThrow();
            screen.hoveredSlot = slot;
            check(screen.mouseClicked(new MouseButtonEvent(screen.leftPos + slot.x + 8, screen.topPos + slot.y + 8,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false), "Space-click through the inventory screen");
        });
        context.getInput().releaseKey(InputConstants.KEY_SPACE);
    }

    private static void checkCursor(ClientGameTestContext context, TestSingleplayerContext world, Item item, int count) {
        context.runOnClient(client -> check(client.player.containerMenu.getCarried().getCount() == count,
                "Client cursor: expected " + count + " " + item + ", got " + client.player.containerMenu.getCarried()));
        world.getServer().runOnServer(server -> {
            var carried = world.getConnection().getServerPlayer().containerMenu.getCarried();
            check(carried.getCount() == count && (count == 0 || carried.is(item)), "Server cursor matches the client");
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
