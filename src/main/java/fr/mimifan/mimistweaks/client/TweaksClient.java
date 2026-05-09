package fr.mimifan.mimistweaks.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.mimifan.mimistweaks.MimisTweaks;
import fr.mimifan.mimistweaks.client.tweaks.AutoClickTweak;
import fr.mimifan.mimistweaks.client.tweaks.AutoFishingTweak;
import fr.mimifan.mimistweaks.client.tweaks.AutoToolTweak;
import fr.mimifan.mimistweaks.client.tweaks.FullbrightTweak;
import fr.mimifan.mimistweaks.client.tweaks.FreecamTweak;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortTweak;
import fr.mimifan.mimistweaks.client.tweaks.InventoryMouseTweaksTweak;
import fr.mimifan.mimistweaks.client.tweaks.TargetInfoTweak;
import fr.mimifan.mimistweaks.client.tweaks.XRayTweak;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import fr.mimifan.mimistweaks.client.tweaks.ContainerSortButtonsHandler;
import fr.mimifan.mimistweaks.client.tweaks.ZoomTweak;
import net.minecraft.world.inventory.AbstractContainerMenu;
import fr.mimifan.mimistweaks.screens.TweaksConfigScreen;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import fr.mimifan.mimistweaks.utils.ConfigPersistence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = MimisTweaks.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TweaksClient {

    public enum Tweak { AUTO_FISHING, AUTO_CLICK, AUTO_TOOL, FREECAM, ZOOM, FULLBRIGHT, SORT_INVENTORY, MOUSE_TWEAKS, XRAY, TARGET_INFO }

    private static final AutoFishingTweak AUTO_FISHING = new AutoFishingTweak();
    private static final AutoClickTweak AUTO_CLICK = new AutoClickTweak();
    private static final AutoToolTweak AUTO_TOOL = new AutoToolTweak();
    private static final FreecamTweak FREECAM = new FreecamTweak();
    private static final ZoomTweak ZOOM = new ZoomTweak();
    private static final FullbrightTweak FULLBRIGHT = new FullbrightTweak();
    private static final InventorySortTweak SORT_INVENTORY_TWEAK = new InventorySortTweak();
    private static final InventoryMouseTweaksTweak MOUSE_TWEAKS = new InventoryMouseTweaksTweak();
    private static final XRayTweak XRAY_TWEAK = new XRayTweak();
    private static final TargetInfoTweak TARGET_INFO_TWEAK = new TargetInfoTweak();

    private static final Set<Integer> prevDownKeys = new HashSet<>();
    private static boolean listenersRegistered = false;

    private TweaksClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ConfigPersistence.load();
        TweaksClientSettings.setConfigKeyCode(MimisKeybinds.getConfigKeyCode());
        TARGET_INFO_TWEAK.syncEnabledFromSettings();
        if (!listenersRegistered) {
            NeoForge.EVENT_BUS.addListener(TweaksClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(TweaksClient::onMouseScroll);
            NeoForge.EVENT_BUS.addListener(TweaksClient::onComputeFov);
            NeoForge.EVENT_BUS.addListener(TweaksClient::onRenderGui);
            listenersRegistered = true;
        }
    }

    public static boolean isTweakEnabled(Tweak tweak) {
        return switch (tweak) {
            case AUTO_FISHING    -> AUTO_FISHING.isEnabled();
            case AUTO_CLICK      -> AUTO_CLICK.isEnabled();
            case AUTO_TOOL       -> AUTO_TOOL.isEnabled();
            case FREECAM         -> FREECAM.isEnabled();
            case ZOOM            -> ZOOM.isEnabled();
            case FULLBRIGHT      -> FULLBRIGHT.isEnabled();
            case SORT_INVENTORY  -> SORT_INVENTORY_TWEAK.isEnabled();
            case MOUSE_TWEAKS    -> MOUSE_TWEAKS.isEnabled();
            case XRAY            -> XRAY_TWEAK.isEnabled();
            case TARGET_INFO     -> TARGET_INFO_TWEAK.isEnabled();
        };
    }

    public static void setTweakEnabledFromUi(Tweak tweak, boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        switch (tweak) {
            case AUTO_FISHING   -> AUTO_FISHING.setEnabled(enabled, player, mc);
            case AUTO_CLICK     -> AUTO_CLICK.setEnabled(enabled, player, mc);
            case AUTO_TOOL      -> AUTO_TOOL.setEnabled(enabled, player, mc);
            case FREECAM        -> FREECAM.setEnabled(enabled, player, mc);
            case ZOOM           -> { /* hold-only */ }
            case FULLBRIGHT     -> FULLBRIGHT.setEnabled(enabled, player, mc);
            case SORT_INVENTORY -> SORT_INVENTORY_TWEAK.setEnabled(enabled, player, mc);
            case MOUSE_TWEAKS   -> MOUSE_TWEAKS.setEnabled(enabled, player, mc);
            case XRAY           -> XRAY_TWEAK.setEnabled(enabled, player, mc);
            case TARGET_INFO    -> TARGET_INFO_TWEAK.setEnabled(enabled, player, mc);
        }
    }

    /** Trigger a sort of the player's own inventory using the configured mode/order.
     *  Only works when the player inventory screen is open (to avoid dangling cursor items). */
    public static void triggerPlayerSort() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        // The player inventory screen must be open so the guard in InventorySortTweak
        // can verify the containerId each tick and abort safely if the screen closes.
        if (!(mc.screen instanceof InventoryScreen)) return;
        SORT_INVENTORY_TWEAK.triggerPlayerSort(mc, player,
                TweaksClientSettings.getSortMode(),
                TweaksClientSettings.getSortOrder());
    }

    /** Trigger a sort of an open container (called from ContainerSortButtonsHandler). */
    public static void triggerContainerSort(AbstractContainerMenu menu, int containerSlotCount) {
        SORT_INVENTORY_TWEAK.triggerContainerSort(menu, containerSlotCount,
                TweaksClientSettings.getSortMode(),
                TweaksClientSettings.getSortOrder());
    }

    /** Trigger a sort based on the currently open container screen (called from the sort keybind). */
    public static void triggerSortForScreen(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) {
            triggerPlayerSort();
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!ContainerSortButtonsHandler.isStorageMenu(menu)) return;
        int containerSize = ContainerSortButtonsHandler.getContainerSize(menu);
        if (containerSize > 0) {
            triggerContainerSort(menu, containerSize);
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        FREECAM.onClientLifecycleTick(mc);
        if (player == null || mc.gameMode == null) {
            return;
        }
        // Keep only the config-menu key synced with Minecraft Controls.
        TweaksClientSettings.setConfigKeyCode(MimisKeybinds.getConfigKeyCode());
        if (mc.screen != null) {
            // Allow the sort keybind to trigger a sort while a container/inventory screen is open.
            if (SORT_INVENTORY_TWEAK.isEnabled()
                    && mc.screen instanceof AbstractContainerScreen<?> cs
                    && consumeKeyPress(TweaksClientSettings.getSortKeyCode())) {
                triggerSortForScreen(cs);
            }
            // Ignore all other hotkeys while a GUI is open.
            syncTrackedKeyStateToCurrent();
            ZOOM.setHeld(false);
        } else {
            if (consumeKeyPress(TweaksClientSettings.getConfigKeyCode())) {
                mc.setScreen(new TweaksConfigScreen(mc.screen));
            }

            if (consumeKeyPress(TweaksClientSettings.getFishingKeyCode())) {
                AUTO_FISHING.setEnabled(!AUTO_FISHING.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getAutoClickKeyCode())) {
                AUTO_CLICK.setEnabled(!AUTO_CLICK.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getAutoToolKeyCode())) {
                AUTO_TOOL.setEnabled(!AUTO_TOOL.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getFreecamKeyCode())) {
                FREECAM.setEnabled(!FREECAM.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getFullbrightKeyCode())) {
                FULLBRIGHT.setEnabled(!FULLBRIGHT.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getXRayKeyCode())) {
                XRAY_TWEAK.setEnabled(!XRAY_TWEAK.isEnabled(), player, mc);
            }

            if (consumeKeyPress(TweaksClientSettings.getTargetInfoKeyCode())) {
                TARGET_INFO_TWEAK.setEnabled(!TARGET_INFO_TWEAK.isEnabled(), player, mc);
            }

            if (SORT_INVENTORY_TWEAK.isEnabled()
                    && consumeKeyPress(TweaksClientSettings.getSortKeyCode())) {
                triggerPlayerSort();
            }

            ZOOM.setHeld(isKeyHeld(TweaksClientSettings.getZoomKeyCode()));
        }

        AUTO_FISHING.onClientTick(mc, player);
        AUTO_CLICK.onClientTick(mc, player);
        AUTO_TOOL.onClientTick(mc, player);
        FREECAM.onClientTick(mc, player);
        FULLBRIGHT.onClientTick(mc, player);
        XRAY_TWEAK.onClientTick(mc, player);
        SORT_INVENTORY_TWEAK.onClientTick(mc, player);
        TARGET_INFO_TWEAK.onClientTick(mc, player);
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        ZOOM.onMouseScroll(event, mc, player);
    }

    private static void onComputeFov(ComputeFovModifierEvent event) {
        ZOOM.onComputeFov(event);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        TARGET_INFO_TWEAK.onRenderGui(event.getGuiGraphics(), Minecraft.getInstance());
    }

    private static boolean consumeKeyPress(int keyCode) {
        if (TweaksClientSettings.isUnboundKeyCode(keyCode)) {
            return false;
        }

        boolean isDown = isInputDown(keyCode);
        boolean wasDown = prevDownKeys.contains(keyCode);
        if (isDown) {
            prevDownKeys.add(keyCode);
        } else {
            prevDownKeys.remove(keyCode);
        }
        return isDown && !wasDown;
    }

    private static void syncTrackedKeyStateToCurrent() {
        syncTrackedKeyState(TweaksClientSettings.getConfigKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getFishingKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getAutoClickKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getAutoToolKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getFreecamKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getFullbrightKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getXRayKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getSortKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getZoomKeyCode());
        syncTrackedKeyState(TweaksClientSettings.getTargetInfoKeyCode());
    }

    private static void syncTrackedKeyState(int keyCode) {
        if (TweaksClientSettings.isUnboundKeyCode(keyCode)) {
            return;
        }

        if (isKeyHeld(keyCode)) {
            prevDownKeys.add(keyCode);
        } else {
            prevDownKeys.remove(keyCode);
        }
    }

    public static boolean isKeyHeld(int keyCode) {
        if (TweaksClientSettings.isUnboundKeyCode(keyCode)) {
            return false;
        }
        return isInputDown(keyCode);
    }

    private static boolean isInputDown(int keyCode) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (TweaksClientSettings.isMouseKeyCode(keyCode)) {
            int mouseButton = TweaksClientSettings.decodeMouseButton(keyCode);
            return GLFW.glfwGetMouseButton(window, mouseButton) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, keyCode);
    }

    public static boolean shouldStopForOpenedScreen(Minecraft mc, boolean stopOnScreen) {
        return stopOnScreen && mc.screen != null && !(mc.screen instanceof ChatScreen);
    }

    public static boolean shouldStopForUnfocusedWindow(Minecraft mc, boolean allowWhenUnfocused) {
        return !allowWhenUnfocused && !mc.isWindowActive();
    }
}

