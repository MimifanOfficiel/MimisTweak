package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.MimisTweaks;
import fr.mimifan.mimistweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(
        modid = MimisTweaks.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.FORGE
)
public final class InventoryMouseTweaksHandler {

    private static final Set<Integer> dragVisitedSlots = new HashSet<>();

    private static boolean shiftDragActive = false;
    private static boolean collectDragActive = false;
    private static boolean collectPerformed = false;

    private static int activeContainerId = -1;

    private InventoryMouseTweaksHandler() {}

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) return;

        if (!TweaksClient.isTweakEnabled(TweaksClient.Tweak.MOUSE_TWEAKS)) {
            return;
        }

        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        Slot slot = findSlotUnderMouse(
                containerScreen,
                event.getMouseX(),
                event.getMouseY()
        );

        if (slot == null) {
            return;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();

        /*
         * SHIFT DRAG
         */
        if (Screen.hasShiftDown()) {

            if (!canQuickMove(slot, player)) {
                return;
            }

            beginShiftDrag(menu);

            quickMove(menu, slot.index, player);

            dragVisitedSlots.add(slot.index);

            event.setCanceled(true);
            return;
        }

        /*
         * COLLECT DRAG
         */
        if (menu.getCarried().isEmpty() && canCollectStart(slot, player)) {

            // Pickup initial manuel
            pickupSlot(menu, slot.index, player);

            beginCollectDrag(menu);

            dragVisitedSlots.add(slot.index);

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {

        if (!shiftDragActive && !collectDragActive) {
            return;
        }

        if (!TweaksClient.isTweakEnabled(TweaksClient.Tweak.MOUSE_TWEAKS)) {
            stopAllDragging();
            return;
        }

        Screen screen = event.getScreen();

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            stopAllDragging();
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            stopAllDragging();
            return;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();

        if (menu.containerId != activeContainerId) {
            stopAllDragging();
            return;
        }

        Slot slot = findSlotUnderMouse(
                containerScreen,
                event.getMouseX(),
                event.getMouseY()
        );

        if (slot == null) {
            event.setCanceled(true);
            return;
        }

        /*
         * SHIFT DRAG
         */
        if (shiftDragActive) {

            if (!Screen.hasShiftDown()) {
                stopShiftDrag();
                return;
            }

            if (!canQuickMove(slot, player)) {
                return;
            }

            if (!dragVisitedSlots.add(slot.index)) {
                return;
            }

            quickMove(menu, slot.index, player);

            event.setCanceled(true);
            return;
        }

        /*
         * COLLECT DRAG
         */
        if (collectDragActive) {

            ItemStack carried = menu.getCarried();

            if (carried.isEmpty()) {
                stopCollectDrag();
                return;
            }

            if (carried.getCount() >= carried.getMaxStackSize()) {
                event.setCanceled(true);
                return;
            }

            if (!canCollectFromSlot(slot, carried, player)) {
                event.setCanceled(true);
                return;
            }

            /*
             * IMPORTANT :
             * On utilise PICKUP_ALL une seule fois
             * pour reproduire le vrai comportement
             * MouseTweaks sans déposer d'items.
             */
            if (!collectPerformed) {

                collectAllToCursor(menu, slot.index, player);

                collectPerformed = true;
            }

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {

        if (event.getButton() != 0) {
            return;
        }

        boolean hadCustomDrag =
                shiftDragActive
                        || collectDragActive;

        stopAllDragging();

        if (hadCustomDrag) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {

        if (!TweaksClient.isTweakEnabled(TweaksClient.Tweak.MOUSE_TWEAKS)) {
            return;
        }

        Screen screen = event.getScreen();

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();

        if (!ContainerSortButtonsHandler.isStorageMenu(menu)) {
            return;
        }

        if (!menu.getCarried().isEmpty()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        Slot hovered = findSlotUnderMouse(
                containerScreen,
                event.getMouseX(),
                event.getMouseY()
        );

        if (hovered == null || !hovered.hasItem()) {
            return;
        }

        int containerSize =
                ContainerSortButtonsHandler.getContainerSize(menu);

        int totalSlots = menu.slots.size();

        if (containerSize <= 0 || containerSize >= totalSlots) {
            return;
        }

        boolean hoveredInContainer =
                hovered.index < containerSize;

        int hoveredStart =
                hoveredInContainer ? 0 : containerSize;

        int hoveredEnd =
                hoveredInContainer ? containerSize : totalSlots;

        int oppositeStart =
                hoveredInContainer ? containerSize : 0;

        int oppositeEnd =
                hoveredInContainer ? totalSlots : containerSize;

        boolean pullToHoveredSide =
                event.getScrollDeltaY() < 0;

        int sourceStart =
                pullToHoveredSide ? oppositeStart : hoveredStart;

        int sourceEnd =
                pullToHoveredSide ? oppositeEnd : hoveredEnd;

        int targetStart =
                pullToHoveredSide ? hoveredStart : oppositeStart;

        int targetEnd =
                pullToHoveredSide ? hoveredEnd : oppositeEnd;

        ItemStack filter = hovered.getItem().copy();

        Slot source = findFirstMatchingSourceSlot(
                menu,
                sourceStart,
                sourceEnd,
                filter,
                player
        );

        if (source == null) {
            return;
        }

        Slot target = findBestSingleItemTargetSlot(
                menu,
                targetStart,
                targetEnd,
                filter
        );

        if (target == null) {
            return;
        }

        moveExactlyOneItem(
                menu,
                source.index,
                target.index,
                player
        );

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        stopAllDragging();
    }

    private static void beginShiftDrag(AbstractContainerMenu menu) {

        shiftDragActive = true;
        collectDragActive = false;

        activeContainerId = menu.containerId;

        dragVisitedSlots.clear();
    }

    private static void beginCollectDrag(AbstractContainerMenu menu) {

        collectDragActive = true;
        shiftDragActive = false;

        collectPerformed = false;

        activeContainerId = menu.containerId;

        dragVisitedSlots.clear();
    }

    private static void stopShiftDrag() {

        shiftDragActive = false;

        if (!collectDragActive) {

            activeContainerId = -1;

            dragVisitedSlots.clear();
        }
    }

    private static void stopCollectDrag() {

        collectDragActive = false;

        collectPerformed = false;

        if (!shiftDragActive) {

            activeContainerId = -1;

            dragVisitedSlots.clear();
        }
    }

    private static void stopAllDragging() {

        shiftDragActive = false;
        collectDragActive = false;

        collectPerformed = false;

        activeContainerId = -1;

        dragVisitedSlots.clear();
    }

    private static boolean canQuickMove(Slot slot, LocalPlayer player) {

        return slot != null
                && slot.hasItem()
                && slot.mayPickup(player);
    }

    private static boolean canCollectStart(Slot slot, LocalPlayer player) {

        return slot.hasItem()
                && slot.mayPickup(player);
    }

    private static boolean canCollectFromSlot(
            Slot slot,
            ItemStack carried,
            LocalPlayer player
    ) {

        if (!slot.hasItem()) {
            return false;
        }

        if (!slot.mayPickup(player)) {
            return false;
        }

        return ItemStack.isSameItemSameTags(
                slot.getItem(),
                carried
        );
    }

    private static void quickMove(
            AbstractContainerMenu menu,
            int slotIndex,
            LocalPlayer player
    ) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.gameMode == null) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                slotIndex,
                0,
                ClickType.QUICK_MOVE,
                player
        );
    }

    private static void pickupSlot(
            AbstractContainerMenu menu,
            int slotIndex,
            LocalPlayer player
    ) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.gameMode == null) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                slotIndex,
                0,
                ClickType.PICKUP,
                player
        );
    }

    private static void collectAllToCursor(
            AbstractContainerMenu menu,
            int slotIndex,
            LocalPlayer player
    ) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.gameMode == null) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                slotIndex,
                0,
                ClickType.PICKUP_ALL,
                player
        );
    }

    private static void moveExactlyOneItem(
            AbstractContainerMenu menu,
            int sourceIndex,
            int targetIndex,
            LocalPlayer player
    ) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.gameMode == null) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                sourceIndex,
                0,
                ClickType.PICKUP,
                player
        );

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                targetIndex,
                1,
                ClickType.PICKUP,
                player
        );

        mc.gameMode.handleInventoryMouseClick(
                menu.containerId,
                sourceIndex,
                0,
                ClickType.PICKUP,
                player
        );
    }

    private static Slot findFirstMatchingSourceSlot(
            AbstractContainerMenu menu,
            int start,
            int end,
            ItemStack filter,
            LocalPlayer player
    ) {

        for (int i = start; i < end; i++) {

            Slot slot = menu.slots.get(i);

            ItemStack stack = slot.getItem();

            if (stack.isEmpty()) {
                continue;
            }

            if (!slot.mayPickup(player)) {
                continue;
            }

            if (ItemStack.isSameItemSameTags(stack, filter)) {
                return slot;
            }
        }

        return null;
    }

    private static Slot findBestSingleItemTargetSlot(
            AbstractContainerMenu menu,
            int start,
            int end,
            ItemStack itemToMove
    ) {

        for (int i = start; i < end; i++) {

            Slot slot = menu.slots.get(i);

            ItemStack stack = slot.getItem();

            if (stack.isEmpty()) {
                continue;
            }

            if (!slot.mayPlace(itemToMove)) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(stack, itemToMove)) {
                continue;
            }

            int max = Math.min(
                    slot.getMaxStackSize(stack),
                    stack.getMaxStackSize()
            );

            if (stack.getCount() < max) {
                return slot;
            }
        }

        for (int i = start; i < end; i++) {

            Slot slot = menu.slots.get(i);

            if (!slot.getItem().isEmpty()) {
                continue;
            }

            if (!slot.mayPlace(itemToMove)) {
                continue;
            }

            return slot;
        }

        return null;
    }

    private static Slot findSlotUnderMouse(
            AbstractContainerScreen<?> screen,
            double mouseX,
            double mouseY
    ) {

        int leftPos = getContainerField(
                screen,
                "leftPos",
                (screen.width - 176) / 2
        );

        int topPos = getContainerField(
                screen,
                "topPos",
                (screen.height - 166) / 2
        );

        for (Slot slot : screen.getMenu().slots) {

            int slotX = leftPos + slot.x;
            int slotY = topPos + slot.y;

            if (mouseX >= slotX
                    && mouseX < slotX + 16
                    && mouseY >= slotY
                    && mouseY < slotY + 16) {

                return slot;
            }
        }

        return null;
    }

    private static int getContainerField(
            AbstractContainerScreen<?> screen,
            String name,
            int fallback
    ) {

        try {

            Field f =
                    AbstractContainerScreen.class.getDeclaredField(name);

            f.setAccessible(true);

            return (int) f.get(screen);

        } catch (Exception e) {

            return fallback;
        }
    }
}