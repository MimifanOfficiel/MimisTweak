package fr.mimifan.mimistweaks.screens;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import fr.mimifan.mimistweaks.utils.ConfigPersistence;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TweaksConfigScreen extends Screen {
    private enum Category {
        AUTO_FARM("screen.mimistweaks.category.autofarm"),
        COMBAT("screen.mimistweaks.category.combat"),
        MISC("screen.mimistweaks.category.misc"),
        RENDER("screen.mimistweaks.category.render");

        private final String translationKey;

        Category(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private static final class SavedPos {
        private int x;
        private int y;

        private SavedPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class TweakEntry {
        private final TweaksClient.Tweak tweak;
        private final String nameKey;
        private boolean expanded;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private int headerX;
        private int headerY;
        private int headerWidth;

        private TweakEntry(TweaksClient.Tweak tweak, String nameKey) {
            this.tweak = tweak;
            this.nameKey = nameKey;
        }

        int getKeyCode() {
            return switch (tweak) {
                case AUTO_FISHING    -> TweaksClientSettings.getFishingKeyCode();
                case AUTO_CLICK      -> TweaksClientSettings.getAutoClickKeyCode();
                case AUTO_TOOL       -> TweaksClientSettings.getAutoToolKeyCode();
                case FREECAM         -> TweaksClientSettings.getFreecamKeyCode();
                case ZOOM            -> TweaksClientSettings.getZoomKeyCode();
                case FULLBRIGHT      -> TweaksClientSettings.getFullbrightKeyCode();
                case SORT_INVENTORY  -> TweaksClientSettings.getSortKeyCode();
                case MOUSE_TWEAKS    -> TweaksClientSettings.getMouseTweaksKeyCode();
                case XRAY            -> TweaksClientSettings.getXRayKeyCode();
                case TARGET_INFO     -> TweaksClientSettings.getTargetInfoKeyCode();
            };
        }

        void setKeyCode(int code) {
            switch (tweak) {
                case AUTO_FISHING    -> TweaksClientSettings.setFishingKeyCode(code);
                case AUTO_CLICK      -> TweaksClientSettings.setAutoClickKeyCode(code);
                case AUTO_TOOL       -> TweaksClientSettings.setAutoToolKeyCode(code);
                case FREECAM         -> TweaksClientSettings.setFreecamKeyCode(code);
                case ZOOM            -> TweaksClientSettings.setZoomKeyCode(code);
                case FULLBRIGHT      -> TweaksClientSettings.setFullbrightKeyCode(code);
                case SORT_INVENTORY  -> TweaksClientSettings.setSortKeyCode(code);
                case MOUSE_TWEAKS    -> TweaksClientSettings.setMouseTweaksKeyCode(code);
                case XRAY            -> TweaksClientSettings.setXRayKeyCode(code);
                case TARGET_INFO     -> TweaksClientSettings.setTargetInfoKeyCode(code);
            }
        }
    }

    private static final class CategoryPanel {
        private final Category category;
        private int x;
        private int y;
        private final int width;
        private boolean collapsed;
        private final List<TweakEntry> tweaks = new ArrayList<>();
        private final List<Component> placeholders = new ArrayList<>();

        private CategoryPanel(Category category, int x, int y, int width) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEADER_HEIGHT = 16;
    private static final int PANEL_PADDING = 8;
    private static final int TWEAK_HEADER_HEIGHT = 16;
    private static final int WIDGET_HEIGHT = 20;
    private static final int GAP = 4;

    private static final Map<Category, SavedPos> SAVED_PANEL_POS = new EnumMap<>(Category.class);

    private final Screen parent;
    private final Map<Category, CategoryPanel> panels = new EnumMap<>(Category.class);

    private Category draggingCategory;
    private int dragOffsetX;
    private int dragOffsetY;
    /** Tweak currently waiting for a key press to be bound. */
    private TweakEntry capturingKeyTweak = null;

    private double pendingThreshold = TweaksClientSettings.getBiteDropThreshold();
    private int pendingReelInDelayMs = TweaksClientSettings.getReelInDelayMs();
    private int pendingDelay = TweaksClientSettings.getRecastDelayTicks();
    private int pendingOffsetMs = TweaksClientSettings.getRecastOffsetMs();
    private boolean pendingFishingStopOnOpenedScreen = TweaksClientSettings.isFishingStopOnOpenedScreen();
    private boolean pendingFishingAllowWhenUnfocused = TweaksClientSettings.isFishingAllowWhenUnfocused();

    private int pendingAutoClickMinutes = TweaksClientSettings.getAutoClickDelayMinutes();
    private int pendingAutoClickSeconds = TweaksClientSettings.getAutoClickDelaySeconds();
    private int pendingAutoClickMilliseconds = TweaksClientSettings.getAutoClickDelayMilliseconds();
    private int pendingAutoClickOffsetMs = TweaksClientSettings.getAutoClickOffsetMs();
    private boolean pendingAutoClickContinuous = TweaksClientSettings.isAutoClickContinuous();
    private boolean pendingAutoClickRightClick = TweaksClientSettings.isAutoClickRightClick();
    private boolean pendingAutoClickStopOnOpenedScreen = TweaksClientSettings.isAutoClickStopOnOpenedScreen();
    private boolean pendingAutoClickAllowWhenUnfocused = TweaksClientSettings.isAutoClickAllowWhenUnfocused();

    private boolean pendingAutoToolSearchInventory = TweaksClientSettings.isAutoToolSearchInventory();

    private boolean pendingFreecamStopOnOpenedScreen = TweaksClientSettings.isFreecamStopOnOpenedScreen();
    private boolean pendingFreecamAllowWhenUnfocused = TweaksClientSettings.isFreecamAllowWhenUnfocused();

    private int pendingZoomPercent = TweaksClientSettings.getZoomPercent();
    private int pendingZoomStepPercent = TweaksClientSettings.getZoomScrollStepPercent();
    private final boolean pendingZoomStopOnOpenedScreen = TweaksClientSettings.isZoomStopOnOpenedScreen();
    private final boolean pendingZoomAllowWhenUnfocused = TweaksClientSettings.isZoomAllowWhenUnfocused();

    private int pendingTargetInfoScalePercent = TweaksClientSettings.getTargetInfoScalePercent();
    private int pendingTargetInfoOffsetX = TweaksClientSettings.getTargetInfoOffsetX();
    private int pendingTargetInfoOffsetY = TweaksClientSettings.getTargetInfoOffsetY();
    private TweaksClientSettings.TargetInfoAnchor pendingTargetInfoAnchor = TweaksClientSettings.getTargetInfoAnchor();
    private TweaksClientSettings.TargetInfoTheme pendingTargetInfoTheme = TweaksClientSettings.getTargetInfoTheme();
    private boolean pendingTargetInfoShowBackground = TweaksClientSettings.isTargetInfoShowBackground();
    private int pendingTargetInfoBackgroundOpacity = TweaksClientSettings.getTargetInfoBackgroundOpacity();
    private boolean pendingTargetInfoShowModName = TweaksClientSettings.isTargetInfoShowModName();
    private boolean pendingTargetInfoShowHealth = TweaksClientSettings.isTargetInfoShowHealth();
    private boolean pendingTargetInfoShowPlayerEquipment = TweaksClientSettings.isTargetInfoShowPlayerEquipment();

    public TweaksConfigScreen(Screen parent) {
        super(Component.translatable("screen.mimistweaks.autofishing.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        panels.clear();

        int topY = 30;
        int startX = Math.max(8, this.width / 2 - (PANEL_WIDTH * 4 + 30) / 2);

        createPanel(Category.AUTO_FARM, startX, topY);
        createPanel(Category.COMBAT, startX + PANEL_WIDTH + 10, topY);
        createPanel(Category.MISC, startX + (PANEL_WIDTH + 10) * 2, topY);
        createPanel(Category.RENDER, startX + (PANEL_WIDTH + 10) * 3, topY);

        layoutPanels();
    }

    private void createPanel(Category category, int defaultX, int defaultY) {
        SavedPos saved = SAVED_PANEL_POS.get(category);
        int panelX = saved != null ? saved.x : defaultX;
        int panelY = saved != null ? saved.y : defaultY;

        CategoryPanel panel = new CategoryPanel(category, panelX, panelY, PANEL_WIDTH);
        panels.put(category, panel);

        if (category == Category.AUTO_FARM) {
            TweakEntry autoFishing = new TweakEntry(TweaksClient.Tweak.AUTO_FISHING, "screen.mimistweaks.tweak.autofishing");
            autoFishing.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.AUTO_FISHING);
            autoFishing.widgets.add(addRenderableWidget(new SensitivitySlider(0, 0, 200, WIDGET_HEIGHT)));
            autoFishing.widgets.add(addRenderableWidget(new ReelInDelaySlider(0, 0, 200, WIDGET_HEIGHT)));
            autoFishing.widgets.add(addRenderableWidget(new DelaySlider(0, 0, 200, WIDGET_HEIGHT)));
            autoFishing.widgets.add(addRenderableWidget(new OffsetSlider(0, 0, 200, WIDGET_HEIGHT)));
            autoFishing.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingFishingStopOnOpenedScreen)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autofishing.stop_on_screens"),
                            (button, value) -> {
                                pendingFishingStopOnOpenedScreen = value;
                                applyPendingValues();
                            })));
            autoFishing.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingFishingAllowWhenUnfocused)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autofishing.allow_unfocused"),
                            (button, value) -> {
                                pendingFishingAllowWhenUnfocused = value;
                                applyPendingValues();
                            })));
            panel.tweaks.add(autoFishing);

            TweakEntry autoClick = new TweakEntry(TweaksClient.Tweak.AUTO_CLICK, "screen.mimistweaks.tweak.autoclick");
            autoClick.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.AUTO_CLICK);
            autoClick.widgets.add(addRenderableWidget(new AutoClickMinutesSlider(0, 0, 200, WIDGET_HEIGHT)));
            autoClick.widgets.add(addRenderableWidget(new AutoClickSecondsSlider(0, 0, 200, WIDGET_HEIGHT)));
            autoClick.widgets.add(addRenderableWidget(new AutoClickMillisecondsSlider(0, 0, 200, WIDGET_HEIGHT)));
            autoClick.widgets.add(addRenderableWidget(new AutoClickOffsetSlider(0, 0, 200, WIDGET_HEIGHT)));
            autoClick.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingAutoClickContinuous)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autoclick.continuous"),
                            (button, value) -> {
                                pendingAutoClickContinuous = value;
                                applyPendingValues();
                            })));
            autoClick.widgets.add(addRenderableWidget(CycleButton.<Boolean>builder(
                            v -> Component.translatable(v ? "screen.mimistweaks.autoclick.button.right" : "screen.mimistweaks.autoclick.button.left"))
                    .withValues(false, true)
                    .withInitialValue(pendingAutoClickRightClick)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autoclick.button"),
                            (button, value) -> {
                                pendingAutoClickRightClick = value;
                                applyPendingValues();
                            })));
            autoClick.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingAutoClickStopOnOpenedScreen)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autoclick.stop_on_screens"),
                            (button, value) -> {
                                pendingAutoClickStopOnOpenedScreen = value;
                                applyPendingValues();
                            })));
            autoClick.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingAutoClickAllowWhenUnfocused)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autoclick.allow_unfocused"),
                            (button, value) -> {
                                pendingAutoClickAllowWhenUnfocused = value;
                                applyPendingValues();
                            })));
            panel.tweaks.add(autoClick);

            TweakEntry autoTool = new TweakEntry(TweaksClient.Tweak.AUTO_TOOL, "screen.mimistweaks.tweak.autotool");
            autoTool.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.AUTO_TOOL);
            autoTool.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingAutoToolSearchInventory)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.autotool.search_inventory"),
                            (button, value) -> {
                                pendingAutoToolSearchInventory = value;
                                applyPendingValues();
                            })));
            panel.tweaks.add(autoTool);
        } else if (category == Category.MISC) {
            TweakEntry freecam = new TweakEntry(TweaksClient.Tweak.FREECAM, "screen.mimistweaks.tweak.freecam");
            freecam.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.FREECAM);
            freecam.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingFreecamStopOnOpenedScreen)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.freecam.stop_on_screens"),
                            (button, value) -> {
                                pendingFreecamStopOnOpenedScreen = value;
                                applyPendingValues();
                            })));
            freecam.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingFreecamAllowWhenUnfocused)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.freecam.allow_unfocused"),
                            (button, value) -> {
                                pendingFreecamAllowWhenUnfocused = value;
                                applyPendingValues();
                            })));
            panel.tweaks.add(freecam);

            TweakEntry zoom = new TweakEntry(TweaksClient.Tweak.ZOOM, "screen.mimistweaks.tweak.zoom");
            zoom.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.ZOOM);
            zoom.widgets.add(addRenderableWidget(new ZoomPercentSlider(0, 0, 200, WIDGET_HEIGHT)));
            zoom.widgets.add(addRenderableWidget(new ZoomStepSlider(0, 0, 200, WIDGET_HEIGHT)));
            panel.tweaks.add(zoom);

            TweakEntry sortInv = new TweakEntry(TweaksClient.Tweak.SORT_INVENTORY, "screen.mimistweaks.tweak.sortinventory");
            panel.tweaks.add(sortInv);

            TweakEntry mouseTweaks = new TweakEntry(TweaksClient.Tweak.MOUSE_TWEAKS, "screen.mimistweaks.tweak.mousetweaks");
            panel.tweaks.add(mouseTweaks);
        } else if (category == Category.RENDER) {
            TweakEntry fullbright = new TweakEntry(TweaksClient.Tweak.FULLBRIGHT, "screen.mimistweaks.tweak.fullbright");
            fullbright.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.FULLBRIGHT);
            panel.tweaks.add(fullbright);

            TweakEntry xray = new TweakEntry(TweaksClient.Tweak.XRAY, "screen.mimistweaks.tweak.xray");
            xray.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.XRAY);
            xray.widgets.add(addRenderableWidget(Button.builder(
                    Component.translatable("screen.mimistweaks.xray.btn.configure"),
                    b -> minecraft.setScreen(new XRayBlockListScreen(this)))
                    .bounds(0, 0, 200, WIDGET_HEIGHT).build()));
            panel.tweaks.add(xray);

            TweakEntry targetInfo = new TweakEntry(TweaksClient.Tweak.TARGET_INFO, "screen.mimistweaks.tweak.targetinfo");
            targetInfo.expanded = TweaksClientSettings.getTweakExpanded(TweaksClient.Tweak.TARGET_INFO);
            targetInfo.widgets.add(addRenderableWidget(new TargetInfoScaleSlider(0, 0, 200, WIDGET_HEIGHT)));
            targetInfo.widgets.add(addRenderableWidget(new TargetInfoOffsetXSlider(0, 0, 200, WIDGET_HEIGHT)));
            targetInfo.widgets.add(addRenderableWidget(new TargetInfoOffsetYSlider(0, 0, 200, WIDGET_HEIGHT)));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.<TweaksClientSettings.TargetInfoAnchor>builder(v -> Component.translatable("screen.mimistweaks.targetinfo.anchor." + v.name().toLowerCase()))
                    .withValues(List.of(TweaksClientSettings.TargetInfoAnchor.values()))
                    .withInitialValue(pendingTargetInfoAnchor)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.anchor"),
                            (button, value) -> {
                                pendingTargetInfoAnchor = value;
                                applyPendingValues();
                            })));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.<TweaksClientSettings.TargetInfoTheme>builder(v -> Component.translatable("screen.mimistweaks.targetinfo.theme." + v.name().toLowerCase()))
                    .withValues(List.of(TweaksClientSettings.TargetInfoTheme.values()))
                    .withInitialValue(pendingTargetInfoTheme)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.theme"),
                            (button, value) -> {
                                pendingTargetInfoTheme = value;
                                applyPendingValues();
                            })));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingTargetInfoShowBackground)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.background"),
                            (button, value) -> {
                                pendingTargetInfoShowBackground = value;
                                applyPendingValues();
                            })));
            targetInfo.widgets.add(addRenderableWidget(new TargetInfoBackgroundOpacitySlider(0, 0, 200, WIDGET_HEIGHT)));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingTargetInfoShowModName)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.show_mod"),
                            (button, value) -> {
                                pendingTargetInfoShowModName = value;
                                applyPendingValues();
                            })));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingTargetInfoShowHealth)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.show_health"),
                            (button, value) -> {
                                pendingTargetInfoShowHealth = value;
                                applyPendingValues();
                            })));
            targetInfo.widgets.add(addRenderableWidget(CycleButton.onOffBuilder(pendingTargetInfoShowPlayerEquipment)
                    .create(0, 0, 200, WIDGET_HEIGHT, Component.translatable("screen.mimistweaks.targetinfo.show_player_equipment"),
                            (button, value) -> {
                                pendingTargetInfoShowPlayerEquipment = value;
                                applyPendingValues();
                            })));
            panel.tweaks.add(targetInfo);
        } else {
            panel.placeholders.add(Component.translatable("screen.mimistweaks.combat.placeholder"));
            panel.placeholders.add(Component.translatable("screen.mimistweaks.combat.placeholder2"));
        }
    }

    private void layoutPanels() {
        for (Category category : Category.values()) {
            CategoryPanel panel = panels.get(category);
            if (panel == null) {
                continue;
            }
            layoutPanel(panel);
        }
    }

    private void layoutPanel(CategoryPanel panel) {
        int rowX = panel.x + PANEL_PADDING;
        int rowWidth = panel.width - PANEL_PADDING * 2;
        int y = panel.y + PANEL_HEADER_HEIGHT + 6;

        for (TweakEntry tweak : panel.tweaks) {
            tweak.headerX = rowX;
            tweak.headerY = y;
            tweak.headerWidth = rowWidth;
            y += TWEAK_HEADER_HEIGHT + GAP;

            boolean showWidgets = !panel.collapsed && tweak.expanded;
            for (AbstractWidget widget : tweak.widgets) {
                widget.visible = showWidgets;
                widget.active = showWidgets;
                if (showWidgets) {
                    widget.setX(rowX + 4);
                    widget.setY(y);
                    widget.setWidth(rowWidth - 8);
                    y += WIDGET_HEIGHT + GAP;
                }
            }
        }
    }

    private int getPanelHeight(CategoryPanel panel) {
        if (panel.collapsed) {
            return PANEL_HEADER_HEIGHT + 6;
        }

        int h = PANEL_HEADER_HEIGHT + 6;

        if (!panel.tweaks.isEmpty()) {
            for (TweakEntry tweak : panel.tweaks) {
                h += TWEAK_HEADER_HEIGHT + GAP;
                if (tweak.expanded) {
                    h += tweak.widgets.size() * (WIDGET_HEIGHT + GAP);
                }
            }
            return h + 4;
        }

        int rows = panel.placeholders.size();
        if (rows == 0) {
            return h + 8;
        }
        return h + rows * 12 + 8;
    }

    private void applyPendingValues() {
        TweaksClientSettings.setBiteDropThreshold(pendingThreshold);
        TweaksClientSettings.setReelInDelayMs(pendingReelInDelayMs);
        TweaksClientSettings.setRecastDelayTicks(pendingDelay);
        TweaksClientSettings.setRecastOffsetMs(pendingOffsetMs);
        TweaksClientSettings.setFishingStopOnOpenedScreen(pendingFishingStopOnOpenedScreen);
        TweaksClientSettings.setFishingAllowWhenUnfocused(pendingFishingAllowWhenUnfocused);

        TweaksClientSettings.setAutoClickDelayMinutes(pendingAutoClickMinutes);
        TweaksClientSettings.setAutoClickDelaySeconds(pendingAutoClickSeconds);
        TweaksClientSettings.setAutoClickDelayMilliseconds(pendingAutoClickMilliseconds);
        TweaksClientSettings.setAutoClickOffsetMs(pendingAutoClickOffsetMs);
        TweaksClientSettings.setAutoClickContinuous(pendingAutoClickContinuous);
        TweaksClientSettings.setAutoClickRightClick(pendingAutoClickRightClick);
        TweaksClientSettings.setAutoClickStopOnOpenedScreen(pendingAutoClickStopOnOpenedScreen);
        TweaksClientSettings.setAutoClickAllowWhenUnfocused(pendingAutoClickAllowWhenUnfocused);

        TweaksClientSettings.setAutoToolSearchInventory(pendingAutoToolSearchInventory);

        TweaksClientSettings.setFreecamStopOnOpenedScreen(pendingFreecamStopOnOpenedScreen);
        TweaksClientSettings.setFreecamAllowWhenUnfocused(pendingFreecamAllowWhenUnfocused);

        TweaksClientSettings.setZoomPercent(pendingZoomPercent);
        TweaksClientSettings.setZoomScrollStepPercent(pendingZoomStepPercent);
        TweaksClientSettings.setZoomStopOnOpenedScreen(pendingZoomStopOnOpenedScreen);
        TweaksClientSettings.setZoomAllowWhenUnfocused(pendingZoomAllowWhenUnfocused);

        TweaksClientSettings.setTargetInfoScalePercent(pendingTargetInfoScalePercent);
        TweaksClientSettings.setTargetInfoOffsetX(pendingTargetInfoOffsetX);
        TweaksClientSettings.setTargetInfoOffsetY(pendingTargetInfoOffsetY);
        TweaksClientSettings.setTargetInfoAnchor(pendingTargetInfoAnchor);
        TweaksClientSettings.setTargetInfoTheme(pendingTargetInfoTheme);
        TweaksClientSettings.setTargetInfoShowBackground(pendingTargetInfoShowBackground);
        TweaksClientSettings.setTargetInfoBackgroundOpacity(pendingTargetInfoBackgroundOpacity);
        TweaksClientSettings.setTargetInfoShowModName(pendingTargetInfoShowModName);
        TweaksClientSettings.setTargetInfoShowHealth(pendingTargetInfoShowHealth);
        TweaksClientSettings.setTargetInfoShowPlayerEquipment(pendingTargetInfoShowPlayerEquipment);

        ConfigPersistence.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKeyTweak != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingKeyTweak.setKeyCode(TweaksClientSettings.KEY_UNBOUND);
                ConfigPersistence.save();
                capturingKeyTweak = null;
                return true;
            }
            capturingKeyTweak.setKeyCode(keyCode);
            ConfigPersistence.save();
            capturingKeyTweak = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (capturingKeyTweak != null) {
            capturingKeyTweak.setKeyCode(TweaksClientSettings.encodeMouseButton(button));
            ConfigPersistence.save();
            capturingKeyTweak = null;
            return true;
        }

        if (button == 1) {
            for (Category category : Category.values()) {
                CategoryPanel panel = panels.get(category);
                if (panel == null) {
                    continue;
                }

                if (isInside(mouseX, mouseY, panel.x, panel.y, panel.width, PANEL_HEADER_HEIGHT)) {
                    panel.collapsed = !panel.collapsed;
                    layoutPanel(panel);
                    return true;
                }

                if (!panel.collapsed) {
                    for (TweakEntry tweak : panel.tweaks) {
                    if (isInside(mouseX, mouseY, tweak.headerX, tweak.headerY, tweak.headerWidth, TWEAK_HEADER_HEIGHT)) {
                            tweak.expanded = !tweak.expanded;
                            TweaksClientSettings.setTweakExpanded(tweak.tweak, tweak.expanded);
                            ConfigPersistence.save();
                            layoutPanel(panel);
                            return true;
                        }
                    }
                }
            }
        }

        if (button == 0) {
            for (Category category : Category.values()) {
                CategoryPanel panel = panels.get(category);
                if (panel == null) {
                    continue;
                }

                if (isInside(mouseX, mouseY, panel.x, panel.y, panel.width, PANEL_HEADER_HEIGHT)) {
                    draggingCategory = category;
                    dragOffsetX = (int) mouseX - panel.x;
                    dragOffsetY = (int) mouseY - panel.y;
                    return true;
                }

                if (!panel.collapsed) {
                    for (TweakEntry tweak : panel.tweaks) {
                        if (isInside(mouseX, mouseY, tweak.headerX, tweak.headerY, tweak.headerWidth, TWEAK_HEADER_HEIGHT)) {
                            // Click on key-bind area (right portion)?
                            int keyAreaLeft = tweak.headerX + tweak.headerWidth - 68;
                            int keyAreaRight = tweak.headerX + tweak.headerWidth - 18;
                            if (mouseX >= keyAreaLeft && mouseX <= keyAreaRight) {
                                capturingKeyTweak = tweak;
                                return true;
                            }
                            // Otherwise toggle tweak
                            boolean nextState = !TweaksClient.isTweakEnabled(tweak.tweak);
                            TweaksClient.setTweakEnabledFromUi(tweak.tweak, nextState);
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingCategory != null) {
            CategoryPanel panel = panels.get(draggingCategory);
            if (panel != null) {
                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;
                int maxX = Math.max(0, this.width - panel.width);
                int maxY = Math.max(0, this.height - getPanelHeight(panel) - 8);

                panel.x = Mth.clamp(newX, 0, maxX);
                panel.y = Mth.clamp(newY, 18, maxY);
                layoutPanel(panel);
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingCategory != null) {
            CategoryPanel panel = panels.get(draggingCategory);
            if (panel != null) {
                SAVED_PANEL_POS.put(draggingCategory, new SavedPos(panel.x, panel.y));
            }
            draggingCategory = null;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Apply values live so settings are saved without any confirmation button.
        applyPendingValues();
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        for (Category category : Category.values()) {
            CategoryPanel panel = panels.get(category);
            if (panel == null) {
                continue;
            }

            int panelHeight = getPanelHeight(panel);
            int left = panel.x;
            int top = panel.y;
            int right = panel.x + panel.width;
            int bottom = panel.y + panelHeight;

            // Panel background: dark semi-transparent
            guiGraphics.fill(left, top, right, bottom, 0xCC1A1E24);
            // Category header: dark blue
            guiGraphics.fill(left, top, right, top + PANEL_HEADER_HEIGHT, 0xEE1E2D42);
            // Blue accent separator
            guiGraphics.fill(left, top + PANEL_HEADER_HEIGHT, right, top + PANEL_HEADER_HEIGHT + 1, 0xFF71B7FF);
            guiGraphics.drawString(this.font, Component.translatable(category.translationKey), left + 6, top + 4, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, panel.collapsed ? "+" : "-", right - 10, top + 4, 0xFF71B7FF, true);

            if (!panel.collapsed) {
                for (TweakEntry tweak : panel.tweaks) {
                    boolean enabled = TweaksClient.isTweakEnabled(tweak.tweak);
                    boolean capturing = (capturingKeyTweak == tweak);
                    // Enabled: dark green tint, Disabled: dark neutral, Capturing: dark amber
                    int color = capturing ? 0xDD3B2C00 : (enabled ? 0xDD1E3B1E : 0xDD262626);
                    int accent = capturing ? 0xFFFFAA00 : (enabled ? 0xFF5AC85A : 0xFF666666);
                    int textColor = capturing ? 0xFFFFDD88 : (enabled ? 0xFF8FEA8F : 0xFFCCCCCC);
                    int tLeft = tweak.headerX;
                    int tTop = tweak.headerY;
                    int tRight = tweak.headerX + tweak.headerWidth;
                    int tBottom = tweak.headerY + TWEAK_HEADER_HEIGHT;
                    guiGraphics.fill(tLeft, tTop, tRight, tBottom, color);
                    // Left accent bar
                    guiGraphics.fill(tLeft, tTop, tLeft + 2, tBottom, accent);
                    guiGraphics.drawString(this.font, Component.translatable(tweak.nameKey), tLeft + 6, tTop + 4, textColor, true);

                    // Key binding display (right side, before the expand arrow)
                    int kc = tweak.getKeyCode();
                    String keyLabel = capturing ? "Press key or mouse..." : TweaksClientSettings.getKeybindDisplayName(kc);
                    int keyLabelColor = capturing ? 0xFFFFDD88 : 0xFF88AADD;
                    guiGraphics.fill(tRight - 70, tTop + 2, tRight - 17, tBottom - 2, 0xBB000000);
                    guiGraphics.drawString(this.font, "[" + keyLabel + "]", tRight - 68, tTop + 4, keyLabelColor, false);

                    // Expand arrow
                    guiGraphics.drawString(this.font, tweak.expanded ? "▾" : "▸", tRight - 12, tTop + 4, accent, true);
                }

                if (panel.tweaks.isEmpty()) {
                    int textY = panel.y + PANEL_HEADER_HEIGHT + 10;
                    for (Component row : panel.placeholders) {
                        guiGraphics.drawString(this.font, row, panel.x + 8, textY, 0xFFAAAAAA, false);
                        textY += 12;
                    }
                }
            }
        }

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen.mimistweaks.overlay.hint_controls"),
                this.width / 2,
                this.height - 14,
                0xFF71B7FF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private final class SensitivitySlider extends AbstractSliderButton {
        private static final double MIN = 0.03D;
        private static final double MAX = 0.20D;

        private SensitivitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (pendingThreshold - MIN) / (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autofishing.sensitivity", String.format("%.3f", pendingThreshold)));
        }

        @Override
        protected void applyValue() {
            pendingThreshold = Mth.clamp(MIN + (MAX - MIN) * this.value, MIN, MAX);
            updateMessage();
        }
    }

    private final class DelaySlider extends AbstractSliderButton {
        private static final int MIN = 5;
        private static final int MAX = 100;

        private DelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingDelay - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autofishing.delay", pendingDelay));
        }

        @Override
        protected void applyValue() {
            pendingDelay = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class ReelInDelaySlider extends AbstractSliderButton {
        private static final int MIN = 50;
        private static final int MAX = 1000;

        private ReelInDelaySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingReelInDelayMs - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autofishing.reel_in_delay", pendingReelInDelayMs));
        }

        @Override
        protected void applyValue() {
            pendingReelInDelayMs = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class OffsetSlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 2000;

        private OffsetSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingOffsetMs / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autofishing.random_offset", pendingOffsetMs));
        }

        @Override
        protected void applyValue() {
            pendingOffsetMs = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class AutoClickMinutesSlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 59;

        private AutoClickMinutesSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingAutoClickMinutes / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autoclick.minutes", pendingAutoClickMinutes));
        }

        @Override
        protected void applyValue() {
            pendingAutoClickMinutes = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class AutoClickSecondsSlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 59;

        private AutoClickSecondsSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingAutoClickSeconds / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autoclick.seconds", pendingAutoClickSeconds));
        }

        @Override
        protected void applyValue() {
            pendingAutoClickSeconds = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class AutoClickMillisecondsSlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 999;

        private AutoClickMillisecondsSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingAutoClickMilliseconds / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autoclick.milliseconds", pendingAutoClickMilliseconds));
        }

        @Override
        protected void applyValue() {
            pendingAutoClickMilliseconds = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class AutoClickOffsetSlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 2000;

        private AutoClickOffsetSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingAutoClickOffsetMs / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.autoclick.random_offset", pendingAutoClickOffsetMs));
        }

        @Override
        protected void applyValue() {
            pendingAutoClickOffsetMs = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class ZoomPercentSlider extends AbstractSliderButton {
        private static final int MIN = 25;
        private static final int MAX = 120;

        private ZoomPercentSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingZoomPercent - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.zoom.amount", pendingZoomPercent));
        }

        @Override
        protected void applyValue() {
            pendingZoomPercent = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class ZoomStepSlider extends AbstractSliderButton {
        private static final int MIN = 1;
        private static final int MAX = 25;

        private ZoomStepSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingZoomStepPercent - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.zoom.step", pendingZoomStepPercent));
        }

        @Override
        protected void applyValue() {
            pendingZoomStepPercent = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class TargetInfoScaleSlider extends AbstractSliderButton {
        private static final int MIN = 60;
        private static final int MAX = 220;

        private TargetInfoScaleSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingTargetInfoScalePercent - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.targetinfo.scale", pendingTargetInfoScalePercent));
        }

        @Override
        protected void applyValue() {
            pendingTargetInfoScalePercent = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class TargetInfoOffsetXSlider extends AbstractSliderButton {
        private static final int MIN = -500;
        private static final int MAX = 500;

        private TargetInfoOffsetXSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingTargetInfoOffsetX - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.targetinfo.offset_x", pendingTargetInfoOffsetX));
        }

        @Override
        protected void applyValue() {
            pendingTargetInfoOffsetX = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class TargetInfoOffsetYSlider extends AbstractSliderButton {
        private static final int MIN = -300;
        private static final int MAX = 300;

        private TargetInfoOffsetYSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) (pendingTargetInfoOffsetY - MIN) / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.targetinfo.offset_y", pendingTargetInfoOffsetY));
        }

        @Override
        protected void applyValue() {
            pendingTargetInfoOffsetY = Mth.clamp((int) Math.round(MIN + (MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }

    private final class TargetInfoBackgroundOpacitySlider extends AbstractSliderButton {
        private static final int MIN = 0;
        private static final int MAX = 255;

        private TargetInfoBackgroundOpacitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (double) pendingTargetInfoBackgroundOpacity / (double) (MAX - MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.mimistweaks.targetinfo.background_opacity", pendingTargetInfoBackgroundOpacity));
        }

        @Override
        protected void applyValue() {
            pendingTargetInfoBackgroundOpacity = Mth.clamp((int) Math.round((MAX - MIN) * this.value), MIN, MAX);
            updateMessage();
        }
    }
}
