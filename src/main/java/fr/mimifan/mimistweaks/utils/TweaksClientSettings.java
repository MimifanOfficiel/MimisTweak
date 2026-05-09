package fr.mimifan.mimistweaks.utils;

import com.mojang.blaze3d.platform.InputConstants;
import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class TweaksClientSettings {
    public static final int KEY_UNBOUND = -1;
    private static final int MOUSE_KEY_BASE = -1000;

    private static double biteDropThreshold = 0.08D;
    private static int reelInDelayMs = 250;
    private static int recastDelayTicks = 20;
    private static int recastOffsetMs = 0;

    private static int autoClickDelayMinutes = 0;
    private static int autoClickDelaySeconds = 0;
    private static int autoClickDelayMilliseconds = 250;
    private static int autoClickOffsetMs = 0;
    private static boolean autoClickContinuous = false;
    private static boolean autoClickRightClick = false;

    private static boolean fishingStopOnOpenedScreen = false;
    private static boolean fishingAllowWhenUnfocused = true;
    private static boolean autoClickStopOnOpenedScreen = false;
    private static boolean autoClickAllowWhenUnfocused = true;
    private static boolean autoToolSearchInventory = false;
    private static boolean freecamStopOnOpenedScreen = false;
    private static boolean freecamAllowWhenUnfocused = true;
    private static boolean zoomStopOnOpenedScreen = false;
    private static boolean zoomAllowWhenUnfocused = true;

    // Per-tweak key codes (GLFW values, or encoded mouse buttons)
    private static int fishingKeyCode    = 79;  // GLFW_KEY_O
    private static int autoClickKeyCode  = 75;  // GLFW_KEY_K
    private static int autoToolKeyCode   = KEY_UNBOUND;
    private static int configKeyCode     = 80;  // GLFW_KEY_P
    private static int freecamKeyCode    = KEY_UNBOUND;
    private static int zoomKeyCode       = KEY_UNBOUND;
    private static int fullbrightKeyCode = KEY_UNBOUND;

    private static int zoomPercent = 35;
    private static int zoomScrollStepPercent = 5;

    public enum TargetInfoAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER_TOP,
        CENTER_BOTTOM,
        CROSSHAIR
    }

    public enum TargetInfoTheme {
        DARK,
        LIGHT,
        ACCENT
    }

    private static boolean targetInfoEnabled = true;
    private static int targetInfoKeyCode = KEY_UNBOUND;
    private static int targetInfoScalePercent = 100;
    private static int targetInfoOffsetX = 12;
    private static int targetInfoOffsetY = 12;
    private static TargetInfoAnchor targetInfoAnchor = TargetInfoAnchor.CENTER_TOP;
    private static TargetInfoTheme targetInfoTheme = TargetInfoTheme.DARK;
    private static boolean targetInfoShowBackground = true;
    private static int targetInfoBackgroundOpacity = 170;
    private static boolean targetInfoShowModName = true;
    private static boolean targetInfoShowHealth = true;
    private static boolean targetInfoShowPlayerEquipment = true;

    private TweaksClientSettings() {}

    public static double getBiteDropThreshold() {
        return biteDropThreshold;
    }

    public static void setBiteDropThreshold(double value) {
        biteDropThreshold = Mth.clamp(value, 0.03D, 0.20D);
    }

    public static int getReelInDelayMs() {
        return reelInDelayMs;
    }

    public static void setReelInDelayMs(int value) {
        reelInDelayMs = Mth.clamp(value, 50, 1000);
    }

    public static int getRecastDelayTicks() {
        return recastDelayTicks;
    }

    public static void setRecastDelayTicks(int value) {
        recastDelayTicks = Mth.clamp(value, 5, 100);
    }

    public static int getRecastOffsetMs() {
        return recastOffsetMs;
    }

    public static void setRecastOffsetMs(int value) {
        recastOffsetMs = Mth.clamp(value, 0, 2000);
    }

    public static int computeReelInDelayTicks() {
        return Math.max(1, (int) Math.round(reelInDelayMs / 50.0D));
    }

    public static int computeRecastDelayTicks() {
        int baseDelayMs = recastDelayTicks * 50;
        int randomOffsetMs = ThreadLocalRandom.current().nextInt(recastOffsetMs + 1);
        int signedOffsetMs = ThreadLocalRandom.current().nextBoolean() ? randomOffsetMs : -randomOffsetMs;
        int adjustedDelayMs = Math.max(1, baseDelayMs + signedOffsetMs);

        return Math.max(1, (int) Math.round(adjustedDelayMs / 50.0D));
    }

    public static int getAutoClickDelayMinutes() {
        return autoClickDelayMinutes;
    }

    public static void setAutoClickDelayMinutes(int value) {
        autoClickDelayMinutes = Mth.clamp(value, 0, 59);
    }

    public static int getAutoClickDelaySeconds() {
        return autoClickDelaySeconds;
    }

    public static void setAutoClickDelaySeconds(int value) {
        autoClickDelaySeconds = Mth.clamp(value, 0, 59);
    }

    public static int getAutoClickDelayMilliseconds() {
        return autoClickDelayMilliseconds;
    }

    public static void setAutoClickDelayMilliseconds(int value) {
        autoClickDelayMilliseconds = Mth.clamp(value, 0, 999);
    }

    public static int getAutoClickOffsetMs() {
        return autoClickOffsetMs;
    }

    public static void setAutoClickOffsetMs(int value) {
        autoClickOffsetMs = Mth.clamp(value, 0, 2000);
    }

    public static boolean isAutoClickContinuous() {
        return autoClickContinuous;
    }

    public static void setAutoClickContinuous(boolean value) {
        autoClickContinuous = value;
    }

    public static boolean isAutoClickRightClick() {
        return autoClickRightClick;
    }

    public static void setAutoClickRightClick(boolean value) {
        autoClickRightClick = value;
    }

    public static int computeAutoClickDelayTicks() {
        int baseDelayMs = autoClickDelayMinutes * 60_000 + autoClickDelaySeconds * 1_000 + autoClickDelayMilliseconds;
        int safeBaseDelayMs = Math.max(1, baseDelayMs);
        int randomOffsetMs = ThreadLocalRandom.current().nextInt(autoClickOffsetMs + 1);
        int signedOffsetMs = ThreadLocalRandom.current().nextBoolean() ? randomOffsetMs : -randomOffsetMs;
        int adjustedDelayMs = Math.max(1, safeBaseDelayMs + signedOffsetMs);

        return Math.max(1, (int) Math.round(adjustedDelayMs / 50.0D));
    }

    public static boolean isFishingStopOnOpenedScreen() {
        return fishingStopOnOpenedScreen;
    }

    public static void setFishingStopOnOpenedScreen(boolean value) {
        fishingStopOnOpenedScreen = value;
    }

    public static boolean isFishingAllowWhenUnfocused() {
        return fishingAllowWhenUnfocused;
    }

    public static void setFishingAllowWhenUnfocused(boolean value) {
        fishingAllowWhenUnfocused = value;
    }

    public static boolean isAutoClickStopOnOpenedScreen() {
        return autoClickStopOnOpenedScreen;
    }

    public static void setAutoClickStopOnOpenedScreen(boolean value) {
        autoClickStopOnOpenedScreen = value;
    }

    public static boolean isAutoClickAllowWhenUnfocused() {
        return autoClickAllowWhenUnfocused;
    }

    public static void setAutoClickAllowWhenUnfocused(boolean value) {
        autoClickAllowWhenUnfocused = value;
    }


    public static boolean isAutoToolSearchInventory() {
        return autoToolSearchInventory;
    }

    public static void setAutoToolSearchInventory(boolean value) {
        autoToolSearchInventory = value;
    }

    public static boolean isFreecamStopOnOpenedScreen() {
        return freecamStopOnOpenedScreen;
    }

    public static void setFreecamStopOnOpenedScreen(boolean value) {
        freecamStopOnOpenedScreen = value;
    }

    public static boolean isFreecamAllowWhenUnfocused() {
        return freecamAllowWhenUnfocused;
    }

    public static void setFreecamAllowWhenUnfocused(boolean value) {
        freecamAllowWhenUnfocused = value;
    }

    public static boolean isZoomStopOnOpenedScreen() {
        return zoomStopOnOpenedScreen;
    }

    public static void setZoomStopOnOpenedScreen(boolean value) {
        zoomStopOnOpenedScreen = value;
    }

    public static boolean isZoomAllowWhenUnfocused() {
        return zoomAllowWhenUnfocused;
    }

    public static void setZoomAllowWhenUnfocused(boolean value) {
        zoomAllowWhenUnfocused = value;
    }

    public static int getZoomPercent() {
        return zoomPercent;
    }

    public static void setZoomPercent(int value) {
        zoomPercent = Mth.clamp(value, 25, 120);
    }

    public static int getZoomScrollStepPercent() {
        return zoomScrollStepPercent;
    }

    public static void setZoomScrollStepPercent(int value) {
        zoomScrollStepPercent = Mth.clamp(value, 1, 25);
    }

    public static int getFishingKeyCode()         { return fishingKeyCode; }
    public static void setFishingKeyCode(int v)   { fishingKeyCode = sanitizeKeyCode(v); }
    public static int getAutoClickKeyCode()       { return autoClickKeyCode; }
    public static void setAutoClickKeyCode(int v) { autoClickKeyCode = sanitizeKeyCode(v); }
    public static int getAutoToolKeyCode()        { return autoToolKeyCode; }
    public static void setAutoToolKeyCode(int v)  { autoToolKeyCode = sanitizeKeyCode(v); }
    public static int getConfigKeyCode()          { return configKeyCode; }
    public static void setConfigKeyCode(int v)    { configKeyCode = sanitizeKeyCode(v); }
    public static int getFreecamKeyCode()         { return freecamKeyCode; }
    public static void setFreecamKeyCode(int v)   { freecamKeyCode = sanitizeKeyCode(v); }
    public static int getZoomKeyCode()            { return zoomKeyCode; }
    public static void setZoomKeyCode(int v)      { zoomKeyCode = sanitizeKeyCode(v); }
    public static int getFullbrightKeyCode()      { return fullbrightKeyCode; }
    public static void setFullbrightKeyCode(int v){ fullbrightKeyCode = sanitizeKeyCode(v); }

    public static int getTargetInfoKeyCode()       { return targetInfoKeyCode; }
    public static void setTargetInfoKeyCode(int v) { targetInfoKeyCode = sanitizeKeyCode(v); }

    // ── X-Ray key code ───────────────────────────────────────────────────────
    private static int xrayKeyCode = KEY_UNBOUND;
    public static int getXRayKeyCode()           { return xrayKeyCode; }
    public static void setXRayKeyCode(int v)     { xrayKeyCode = sanitizeKeyCode(v); }

    // ── Tweak expanded state ──────────────────────────────────────────────────
    private static final Map<TweaksClient.Tweak, Boolean> tweakExpanded = new EnumMap<>(TweaksClient.Tweak.class);

    public static boolean getTweakExpanded(TweaksClient.Tweak tweak) {
        // AUTO_FISHING is expanded by default, others collapsed
        return tweakExpanded.getOrDefault(tweak, tweak == TweaksClient.Tweak.AUTO_FISHING);
    }

    public static void setTweakExpanded(TweaksClient.Tweak tweak, boolean expanded) {
        tweakExpanded.put(tweak, expanded);
    }

    // ── Sort Inventory ───────────────────────────────────────────────────────
    private static int sortKeyCode = KEY_UNBOUND;
    private static int mouseTweaksKeyCode = KEY_UNBOUND;
    private static InventorySortHelper.SortMode  sortMode  = InventorySortHelper.SortMode.ALPHABETICAL;
    private static InventorySortHelper.SortOrder sortOrder = InventorySortHelper.SortOrder.ASCENDING;

    public static int getSortKeyCode()           { return sortKeyCode; }
    public static void setSortKeyCode(int v)     { sortKeyCode = sanitizeKeyCode(v); }

    public static int getMouseTweaksKeyCode()       { return mouseTweaksKeyCode; }
    public static void setMouseTweaksKeyCode(int v) { mouseTweaksKeyCode = sanitizeKeyCode(v); }

    public static InventorySortHelper.SortMode getSortMode()  { return sortMode; }
    public static void setSortMode(InventorySortHelper.SortMode v) { sortMode = v; }

    public static InventorySortHelper.SortOrder getSortOrder()  { return sortOrder; }
    public static void setSortOrder(InventorySortHelper.SortOrder v) { sortOrder = v; }

    public static boolean isTargetInfoEnabled() {
        return targetInfoEnabled;
    }

    public static void setTargetInfoEnabled(boolean value) {
        targetInfoEnabled = value;
    }

    public static int getTargetInfoScalePercent() {
        return targetInfoScalePercent;
    }

    public static void setTargetInfoScalePercent(int value) {
        targetInfoScalePercent = Mth.clamp(value, 60, 220);
    }

    public static int getTargetInfoOffsetX() {
        return targetInfoOffsetX;
    }

    public static void setTargetInfoOffsetX(int value) {
        targetInfoOffsetX = Mth.clamp(value, -500, 500);
    }

    public static int getTargetInfoOffsetY() {
        return targetInfoOffsetY;
    }

    public static void setTargetInfoOffsetY(int value) {
        targetInfoOffsetY = Mth.clamp(value, -300, 300);
    }

    public static TargetInfoAnchor getTargetInfoAnchor() {
        return targetInfoAnchor;
    }

    public static void setTargetInfoAnchor(TargetInfoAnchor value) {
        targetInfoAnchor = value == null ? TargetInfoAnchor.TOP_LEFT : value;
    }

    public static TargetInfoTheme getTargetInfoTheme() {
        return targetInfoTheme;
    }

    public static void setTargetInfoTheme(TargetInfoTheme value) {
        targetInfoTheme = value == null ? TargetInfoTheme.DARK : value;
    }

    public static boolean isTargetInfoShowBackground() {
        return targetInfoShowBackground;
    }

    public static void setTargetInfoShowBackground(boolean value) {
        targetInfoShowBackground = value;
    }

    public static int getTargetInfoBackgroundOpacity() {
        return targetInfoBackgroundOpacity;
    }

    public static void setTargetInfoBackgroundOpacity(int value) {
        targetInfoBackgroundOpacity = Mth.clamp(value, 0, 255);
    }

    public static boolean isTargetInfoShowModName() {
        return targetInfoShowModName;
    }

    public static void setTargetInfoShowModName(boolean value) {
        targetInfoShowModName = value;
    }

    public static boolean isTargetInfoShowHealth() {
        return targetInfoShowHealth;
    }

    public static void setTargetInfoShowHealth(boolean value) {
        targetInfoShowHealth = value;
    }

    public static boolean isTargetInfoShowPlayerEquipment() {
        return targetInfoShowPlayerEquipment;
    }

    public static void setTargetInfoShowPlayerEquipment(boolean value) {
        targetInfoShowPlayerEquipment = value;
    }

    public static int encodeMouseButton(int mouseButton) {
        return MOUSE_KEY_BASE - Math.max(0, mouseButton);
    }

    public static boolean isUnboundKeyCode(int keyCode) {
        return keyCode == KEY_UNBOUND;
    }

    public static boolean isMouseKeyCode(int keyCode) {
        return keyCode <= MOUSE_KEY_BASE;
    }

    public static int decodeMouseButton(int keyCode) {
        return MOUSE_KEY_BASE - keyCode;
    }

    public static boolean isSupportedKeyCode(int keyCode) {
        return keyCode >= 0 || keyCode == KEY_UNBOUND || isMouseKeyCode(keyCode);
    }

    public static String getKeybindDisplayName(int keyCode) {
        if (isUnboundKeyCode(keyCode)) {
            return "None";
        }
        if (isMouseKeyCode(keyCode)) {
            return InputConstants.Type.MOUSE.getOrCreate(decodeMouseButton(keyCode)).getDisplayName().getString();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }

    private static int sanitizeKeyCode(int keyCode) {
        return isSupportedKeyCode(keyCode) ? keyCode : KEY_UNBOUND;
    }
}
