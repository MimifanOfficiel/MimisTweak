package fr.mimifan.mimistweaks.utils;

import com.google.gson.*;
import fr.mimifan.mimistweaks.MimisTweaks;
import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper;
import fr.mimifan.mimistweaks.client.tweaks.XRayBlockList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.*;

public final class ConfigPersistence {
    private static final String FILE_NAME = "mimistweaks.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigPersistence() {}

    private static Path getConfigPath() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve(FILE_NAME);
    }

    public static void save() {
        JsonObject obj = new JsonObject();
        // Fishing
        obj.addProperty("biteDropThreshold", TweaksClientSettings.getBiteDropThreshold());
        obj.addProperty("reelInDelayMs", TweaksClientSettings.getReelInDelayMs());
        obj.addProperty("recastDelayTicks", TweaksClientSettings.getRecastDelayTicks());
        obj.addProperty("recastOffsetMs", TweaksClientSettings.getRecastOffsetMs());
        obj.addProperty("fishingStopOnOpenedScreen", TweaksClientSettings.isFishingStopOnOpenedScreen());
        obj.addProperty("fishingAllowWhenUnfocused", TweaksClientSettings.isFishingAllowWhenUnfocused());
        // AutoClick
        obj.addProperty("autoClickDelayMinutes", TweaksClientSettings.getAutoClickDelayMinutes());
        obj.addProperty("autoClickDelaySeconds", TweaksClientSettings.getAutoClickDelaySeconds());
        obj.addProperty("autoClickDelayMilliseconds", TweaksClientSettings.getAutoClickDelayMilliseconds());
        obj.addProperty("autoClickOffsetMs", TweaksClientSettings.getAutoClickOffsetMs());
        obj.addProperty("autoClickContinuous", TweaksClientSettings.isAutoClickContinuous());
        obj.addProperty("autoClickRightClick", TweaksClientSettings.isAutoClickRightClick());
        obj.addProperty("autoClickStopOnOpenedScreen", TweaksClientSettings.isAutoClickStopOnOpenedScreen());
        obj.addProperty("autoClickAllowWhenUnfocused", TweaksClientSettings.isAutoClickAllowWhenUnfocused());
        // Freecam
        obj.addProperty("freecamStopOnOpenedScreen", TweaksClientSettings.isFreecamStopOnOpenedScreen());
        obj.addProperty("freecamAllowWhenUnfocused", TweaksClientSettings.isFreecamAllowWhenUnfocused());
        // Zoom
        obj.addProperty("zoomPercent", TweaksClientSettings.getZoomPercent());
        obj.addProperty("zoomScrollStepPercent", TweaksClientSettings.getZoomScrollStepPercent());
        obj.addProperty("zoomStopOnOpenedScreen", TweaksClientSettings.isZoomStopOnOpenedScreen());
        obj.addProperty("zoomAllowWhenUnfocused", TweaksClientSettings.isZoomAllowWhenUnfocused());
        // Target Info
        obj.addProperty("targetInfoEnabled", TweaksClientSettings.isTargetInfoEnabled());
        obj.addProperty("targetInfoScalePercent", TweaksClientSettings.getTargetInfoScalePercent());
        obj.addProperty("targetInfoOffsetX", TweaksClientSettings.getTargetInfoOffsetX());
        obj.addProperty("targetInfoOffsetY", TweaksClientSettings.getTargetInfoOffsetY());
        obj.addProperty("targetInfoAnchor", TweaksClientSettings.getTargetInfoAnchor().name());
        obj.addProperty("targetInfoTheme", TweaksClientSettings.getTargetInfoTheme().name());
        obj.addProperty("targetInfoShowBackground", TweaksClientSettings.isTargetInfoShowBackground());
        obj.addProperty("targetInfoBackgroundOpacity", TweaksClientSettings.getTargetInfoBackgroundOpacity());
        obj.addProperty("targetInfoShowModName", TweaksClientSettings.isTargetInfoShowModName());
        obj.addProperty("targetInfoShowHealth", TweaksClientSettings.isTargetInfoShowHealth());
        obj.addProperty("targetInfoShowPlayerEquipment", TweaksClientSettings.isTargetInfoShowPlayerEquipment());
        // Key codes
        obj.addProperty("fishingKeyCode",   TweaksClientSettings.getFishingKeyCode());
        obj.addProperty("autoClickKeyCode", TweaksClientSettings.getAutoClickKeyCode());
        obj.addProperty("autoToolKeyCode",  TweaksClientSettings.getAutoToolKeyCode());
        obj.addProperty("autoToolSearchInventory",    TweaksClientSettings.isAutoToolSearchInventory());
        obj.addProperty("configKeyCode",    TweaksClientSettings.getConfigKeyCode());
        obj.addProperty("freecamKeyCode",   TweaksClientSettings.getFreecamKeyCode());
        obj.addProperty("zoomKeyCode",      TweaksClientSettings.getZoomKeyCode());
        obj.addProperty("fullbrightKeyCode", TweaksClientSettings.getFullbrightKeyCode());
        obj.addProperty("targetInfoKeyCode", TweaksClientSettings.getTargetInfoKeyCode());
        // Sort Inventory
        obj.addProperty("sortKeyCode",  TweaksClientSettings.getSortKeyCode());
        obj.addProperty("mouseTweaksKeyCode", TweaksClientSettings.getMouseTweaksKeyCode());
        obj.addProperty("sortMode",     TweaksClientSettings.getSortMode().name());
        obj.addProperty("sortOrder",    TweaksClientSettings.getSortOrder().name());
        // X-Ray
        obj.addProperty("xrayKeyCode",  TweaksClientSettings.getXRayKeyCode());
        JsonArray xrayBlocks = new JsonArray();
        for (String id : XRayBlockList.getVisibleIds()) xrayBlocks.add(id);
        obj.add("xrayVisibleBlocks", xrayBlocks);
        // Tweak expanded states
        JsonObject expandedObj = new JsonObject();
        for (TweaksClient.Tweak tweak : TweaksClient.Tweak.values()) {
            expandedObj.addProperty(tweak.name(), TweaksClientSettings.getTweakExpanded(tweak));
        }
        obj.add("tweakExpanded", expandedObj);

        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(obj));
        } catch (IOException e) {
            MimisTweaks.LOGGER.error("[MimisTweaks] Failed to save config: {}", e.getMessage());
        }
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;
        try {
            JsonObject obj = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (obj.has("biteDropThreshold"))          TweaksClientSettings.setBiteDropThreshold(obj.get("biteDropThreshold").getAsDouble());
            if (obj.has("reelInDelayMs"))               TweaksClientSettings.setReelInDelayMs(obj.get("reelInDelayMs").getAsInt());
            if (obj.has("recastDelayTicks"))            TweaksClientSettings.setRecastDelayTicks(obj.get("recastDelayTicks").getAsInt());
            if (obj.has("recastOffsetMs"))              TweaksClientSettings.setRecastOffsetMs(obj.get("recastOffsetMs").getAsInt());
            if (obj.has("fishingStopOnOpenedScreen"))   TweaksClientSettings.setFishingStopOnOpenedScreen(obj.get("fishingStopOnOpenedScreen").getAsBoolean());
            if (obj.has("fishingAllowWhenUnfocused"))   TweaksClientSettings.setFishingAllowWhenUnfocused(obj.get("fishingAllowWhenUnfocused").getAsBoolean());
            if (obj.has("autoClickDelayMinutes"))       TweaksClientSettings.setAutoClickDelayMinutes(obj.get("autoClickDelayMinutes").getAsInt());
            if (obj.has("autoClickDelaySeconds"))       TweaksClientSettings.setAutoClickDelaySeconds(obj.get("autoClickDelaySeconds").getAsInt());
            if (obj.has("autoClickDelayMilliseconds"))  TweaksClientSettings.setAutoClickDelayMilliseconds(obj.get("autoClickDelayMilliseconds").getAsInt());
            if (obj.has("autoClickOffsetMs"))           TweaksClientSettings.setAutoClickOffsetMs(obj.get("autoClickOffsetMs").getAsInt());
            if (obj.has("autoClickContinuous"))         TweaksClientSettings.setAutoClickContinuous(obj.get("autoClickContinuous").getAsBoolean());
            if (obj.has("autoClickRightClick"))          TweaksClientSettings.setAutoClickRightClick(obj.get("autoClickRightClick").getAsBoolean());
            if (obj.has("autoClickStopOnOpenedScreen")) TweaksClientSettings.setAutoClickStopOnOpenedScreen(obj.get("autoClickStopOnOpenedScreen").getAsBoolean());
            if (obj.has("autoClickAllowWhenUnfocused")) TweaksClientSettings.setAutoClickAllowWhenUnfocused(obj.get("autoClickAllowWhenUnfocused").getAsBoolean());
            if (obj.has("autoToolSearchInventory"))      TweaksClientSettings.setAutoToolSearchInventory(obj.get("autoToolSearchInventory").getAsBoolean());
            if (obj.has("freecamStopOnOpenedScreen"))   TweaksClientSettings.setFreecamStopOnOpenedScreen(obj.get("freecamStopOnOpenedScreen").getAsBoolean());
            if (obj.has("freecamAllowWhenUnfocused"))   TweaksClientSettings.setFreecamAllowWhenUnfocused(obj.get("freecamAllowWhenUnfocused").getAsBoolean());
            if (obj.has("zoomPercent"))                 TweaksClientSettings.setZoomPercent(obj.get("zoomPercent").getAsInt());
            if (obj.has("zoomScrollStepPercent"))       TweaksClientSettings.setZoomScrollStepPercent(obj.get("zoomScrollStepPercent").getAsInt());
            if (obj.has("zoomStopOnOpenedScreen"))      TweaksClientSettings.setZoomStopOnOpenedScreen(obj.get("zoomStopOnOpenedScreen").getAsBoolean());
            if (obj.has("zoomAllowWhenUnfocused"))      TweaksClientSettings.setZoomAllowWhenUnfocused(obj.get("zoomAllowWhenUnfocused").getAsBoolean());
            if (obj.has("targetInfoEnabled"))            TweaksClientSettings.setTargetInfoEnabled(obj.get("targetInfoEnabled").getAsBoolean());
            if (obj.has("targetInfoScalePercent"))       TweaksClientSettings.setTargetInfoScalePercent(obj.get("targetInfoScalePercent").getAsInt());
            if (obj.has("targetInfoOffsetX"))            TweaksClientSettings.setTargetInfoOffsetX(obj.get("targetInfoOffsetX").getAsInt());
            if (obj.has("targetInfoOffsetY"))            TweaksClientSettings.setTargetInfoOffsetY(obj.get("targetInfoOffsetY").getAsInt());
            if (obj.has("targetInfoAnchor"))             TweaksClientSettings.setTargetInfoAnchor(TweaksClientSettings.TargetInfoAnchor.valueOf(obj.get("targetInfoAnchor").getAsString()));
            if (obj.has("targetInfoTheme"))              TweaksClientSettings.setTargetInfoTheme(TweaksClientSettings.TargetInfoTheme.valueOf(obj.get("targetInfoTheme").getAsString()));
            if (obj.has("targetInfoShowBackground"))     TweaksClientSettings.setTargetInfoShowBackground(obj.get("targetInfoShowBackground").getAsBoolean());
            if (obj.has("targetInfoBackgroundOpacity"))  TweaksClientSettings.setTargetInfoBackgroundOpacity(obj.get("targetInfoBackgroundOpacity").getAsInt());
            if (obj.has("targetInfoShowModName"))        TweaksClientSettings.setTargetInfoShowModName(obj.get("targetInfoShowModName").getAsBoolean());
            if (obj.has("targetInfoShowHealth"))         TweaksClientSettings.setTargetInfoShowHealth(obj.get("targetInfoShowHealth").getAsBoolean());
            if (obj.has("targetInfoShowPlayerEquipment")) TweaksClientSettings.setTargetInfoShowPlayerEquipment(obj.get("targetInfoShowPlayerEquipment").getAsBoolean());
            if (obj.has("fishingKeyCode"))   TweaksClientSettings.setFishingKeyCode(obj.get("fishingKeyCode").getAsInt());
            if (obj.has("autoClickKeyCode")) TweaksClientSettings.setAutoClickKeyCode(obj.get("autoClickKeyCode").getAsInt());
            if (obj.has("autoToolKeyCode"))  TweaksClientSettings.setAutoToolKeyCode(obj.get("autoToolKeyCode").getAsInt());
            if (obj.has("configKeyCode"))    TweaksClientSettings.setConfigKeyCode(obj.get("configKeyCode").getAsInt());
            if (obj.has("freecamKeyCode"))   TweaksClientSettings.setFreecamKeyCode(obj.get("freecamKeyCode").getAsInt());
            if (obj.has("zoomKeyCode"))      TweaksClientSettings.setZoomKeyCode(obj.get("zoomKeyCode").getAsInt());
            if (obj.has("fullbrightKeyCode")) TweaksClientSettings.setFullbrightKeyCode(obj.get("fullbrightKeyCode").getAsInt());
            if (obj.has("targetInfoKeyCode")) TweaksClientSettings.setTargetInfoKeyCode(obj.get("targetInfoKeyCode").getAsInt());
            if (obj.has("sortKeyCode"))  TweaksClientSettings.setSortKeyCode(obj.get("sortKeyCode").getAsInt());
            if (obj.has("mouseTweaksKeyCode")) TweaksClientSettings.setMouseTweaksKeyCode(obj.get("mouseTweaksKeyCode").getAsInt());
            if (obj.has("sortMode"))     TweaksClientSettings.setSortMode(InventorySortHelper.SortMode.valueOf(obj.get("sortMode").getAsString()));
            if (obj.has("sortOrder"))    TweaksClientSettings.setSortOrder(InventorySortHelper.SortOrder.valueOf(obj.get("sortOrder").getAsString()));
            if (obj.has("xrayKeyCode"))  TweaksClientSettings.setXRayKeyCode(obj.get("xrayKeyCode").getAsInt());
            if (obj.has("xrayVisibleBlocks")) {
                XRayBlockList.clearAll();
                for (JsonElement el : obj.getAsJsonArray("xrayVisibleBlocks")) {
                    XRayBlockList.setVisible(el.getAsString(), true);
                }
            }
            if (obj.has("tweakExpanded")) {
                JsonObject expandedObj = obj.getAsJsonObject("tweakExpanded");
                for (TweaksClient.Tweak tweak : TweaksClient.Tweak.values()) {
                    if (expandedObj.has(tweak.name())) {
                        TweaksClientSettings.setTweakExpanded(tweak, expandedObj.get(tweak.name()).getAsBoolean());
                    }
                }
            }
        } catch (Exception e) {
            MimisTweaks.LOGGER.error("[MimisTweaks] Failed to load config: {}", e.getMessage());
        }
    }
}

