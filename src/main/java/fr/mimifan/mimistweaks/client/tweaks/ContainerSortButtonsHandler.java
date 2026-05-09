package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.MimisTweaks;
import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper.SortMode;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper.SortOrder;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MimisTweaks.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class ContainerSortButtonsHandler {

    private ContainerSortButtonsHandler() {}

    private record SortButton(int x, int y, int w, int h, String label, String tooltipKey, Runnable action) {}

    private static final List<SortButton> buttons = new ArrayList<>();
    /** Track the screen instance so we rebuild on screen change. */
    private static Screen lastScreen = null;

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
        if (!TweaksClient.isTweakEnabled(TweaksClient.Tweak.SORT_INVENTORY)) return;
        if (!supportsSortButtons(cs)) return;

        if (screen != lastScreen) {
            lastScreen = screen;
            buttons.clear();
        }
        rebuildButtons(cs);

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();

        for (SortButton btn : buttons) {
            boolean hovered = isOver(mx, my, btn);
            int bg     = hovered ? 0xDD3A5A8A : 0xDD1E2D42;
            int border = hovered ? 0xFF71B7FF : 0xFF3A5A8A;

            // Background
            gfx.fill(btn.x(), btn.y(), btn.x() + btn.w(), btn.y() + btn.h(), bg);
            // Border (1 px lines)
            gfx.fill(btn.x(),                    btn.y(),                btn.x() + btn.w(),     btn.y() + 1,          border);
            gfx.fill(btn.x(),                    btn.y() + btn.h() - 1, btn.x() + btn.w(),     btn.y() + btn.h(),    border);
            gfx.fill(btn.x(),                    btn.y(),                btn.x() + 1,           btn.y() + btn.h(),    border);
            gfx.fill(btn.x() + btn.w() - 1,     btn.y(),                btn.x() + btn.w(),     btn.y() + btn.h(),    border);
            // Label (centred)
            int tx = btn.x() + (btn.w() - font.width(btn.label())) / 2;
            int ty = btn.y() + (btn.h() - 8) / 2;
            gfx.drawString(font, btn.label(), tx, ty, 0xFFFFFF, false);
        }

        // Tooltip for hovered button
        for (SortButton btn : buttons) {
            if (isOver(mx, my, btn)) {
                gfx.renderTooltip(font, Component.translatable(btn.tooltipKey()), mx, my);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
        if (!TweaksClient.isTweakEnabled(TweaksClient.Tweak.SORT_INVENTORY)) return;
        if (!supportsSortButtons(cs)) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (SortButton btn : buttons) {
            if (mx >= btn.x() && mx <= btn.x() + btn.w()
                    && my >= btn.y() && my <= btn.y() + btn.h()) {
                btn.action().run();
                event.setCanceled(true);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Button building
    // -------------------------------------------------------------------------

    private static void rebuildButtons(AbstractContainerScreen<?> screen) {
        buttons.clear();
        AbstractContainerMenu menu = screen.getMenu();
        boolean playerInventory = screen instanceof InventoryScreen;
        int containerSize = getContainerSize(menu);
        if (!playerInventory && containerSize <= 0) return;

        int leftPos  = getContainerField(screen, "leftPos",   (screen.width  - 176) / 2);
        int topPos   = getContainerField(screen, "topPos",    (screen.height - 166) / 2);
        int imgWidth = getContainerField(screen, "imageWidth", 176);

        // Buttons sit to the right of the container background
        int bx  = leftPos + imgWidth + 6;
        int bw  = 32;
        int bh  = 14;
        int gap = 3;
        int by  = topPos + 4;

        SortMode  mode  = TweaksClientSettings.getSortMode();
        SortOrder order = TweaksClientSettings.getSortOrder();

        // ── A-Z ──────────────────────────────────────────────────────────────
        boolean alphaActive = (mode == SortMode.ALPHABETICAL);
        buttons.add(new SortButton(bx, by, bw, bh,
                alphaActive ? "✔A-Z" : "A-Z",
                "screen.mimistweaks.sort.btn.alphabetical",
                () -> {
                    TweaksClientSettings.setSortMode(SortMode.ALPHABETICAL);
                    triggerSort(menu, containerSize, playerInventory);
                }));

        // ── 1-9 (by count) ───────────────────────────────────────────────────
        by += bh + gap;
        boolean countActive = (mode == SortMode.COUNT);
        buttons.add(new SortButton(bx, by, bw, bh,
                countActive ? "✔1-9" : "1-9",
                "screen.mimistweaks.sort.btn.count",
                () -> {
                    TweaksClientSettings.setSortMode(SortMode.COUNT);
                    triggerSort(menu, containerSize, playerInventory);
                }));

        // ── Cat (by category) ────────────────────────────────────────────────
        by += bh + gap;
        boolean catActive = (mode == SortMode.CATEGORY);
        buttons.add(new SortButton(bx, by, bw, bh,
                catActive ? "✔Cat" : "Cat",
                "screen.mimistweaks.sort.btn.category",
                () -> {
                    TweaksClientSettings.setSortMode(SortMode.CATEGORY);
                    triggerSort(menu, containerSize, playerInventory);
                }));

        // ── ↑ / ↓ (order toggle) ─────────────────────────────────────────────
        by += bh + gap;
        String orderLabel = (order == SortOrder.ASCENDING) ? "↑ Asc" : "↓ Desc";
        buttons.add(new SortButton(bx, by, bw, bh,
                orderLabel,
                "screen.mimistweaks.sort.btn.order",
                () -> {
                    TweaksClientSettings.setSortOrder(TweaksClientSettings.getSortOrder().toggle());
                    triggerSort(menu, containerSize, playerInventory);
                }));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isOver(int mx, int my, SortButton btn) {
        return mx >= btn.x() && mx <= btn.x() + btn.w()
                && my >= btn.y() && my <= btn.y() + btn.h();
    }

    private static boolean supportsSortButtons(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen || isStorageMenu(screen.getMenu());
    }

    private static void triggerSort(AbstractContainerMenu menu, int containerSize, boolean playerInventory) {
        if (playerInventory) {
            TweaksClient.triggerPlayerSort();
            return;
        }
        TweaksClient.triggerContainerSort(menu, containerSize);
    }

    public static boolean isStorageMenu(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu
                || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu
                || menu instanceof DispenserMenu;
    }

    public static int getContainerSize(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu cm) {
            return cm.getRowCount() * 9;
        }
        if (menu instanceof HopperMenu) return 5;
        if (menu instanceof DispenserMenu) return 9;
        if (menu instanceof ShulkerBoxMenu) return 27;
        // Generic fallback: total slots minus the 36 player-inventory slots
        int total = menu.slots.size();
        return Math.max(0, total - 36);
    }

    private static int getContainerField(AbstractContainerScreen<?> screen, String name, int fallback) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return (int) f.get(screen);
        } catch (Exception e) {
            return fallback;
        }
    }
}

