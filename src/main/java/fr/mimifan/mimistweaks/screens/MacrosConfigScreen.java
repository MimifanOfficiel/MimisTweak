package fr.mimifan.mimistweaks.screens;

import fr.mimifan.mimistweaks.utils.ConfigPersistence;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MacrosConfigScreen extends Screen {
    private static final int HEADER_H = 36;
    private static final int FOOTER_H = 28;
    private static final int ROW_H = 28;
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_GAP = 4;

    private final Screen parent;
    private final List<MacroRow> rows = new ArrayList<>();

    private int scrollOffset;
    private MacroRow capturingRow;
    private Button addButton;
    private Button doneButton;
    private boolean draggingScrollbar;
    private int dragStartY;
    private int dragStartScroll;

    public MacrosConfigScreen(Screen parent) {
        super(Component.translatable("screen.mimistweaks.macros.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        boolean removedPlaceholderMacro = false;
        for (TweaksClientSettings.MacroEntry macro : TweaksClientSettings.getMacros()) {
            if (isPlaceholderMacro(macro)) {
                removedPlaceholderMacro = true;
                continue;
            }
            rows.add(new MacroRow(macro.copy()));
        }

        int buttonY = this.height - FOOTER_H + 4;
        addButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.macros.btn.add"),
                b -> {
                    MacroRow row = new MacroRow(new TweaksClientSettings.MacroEntry("", TweaksClientSettings.KEY_UNBOUND, true));
                    rows.add(row);
                    row.attach();
                    saveRows();
                })
                .bounds(this.width / 2 - 134, buttonY, 130, 20)
                .build());

        doneButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.macros.btn.done"),
                b -> onClose())
                .bounds(this.width / 2 + 4, buttonY, 130, 20)
                .build());

        for (MacroRow row : rows) {
            row.attach();
        }

        // Clean up old placeholder entries from config so the screen starts truly empty.
        if (removedPlaceholderMacro) {
            saveRows();
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);

        gfx.fill(0, 0, this.width, this.height, 0xCC101419);

        gfx.fill(0, 0, this.width, HEADER_H + 8, 0xD9161E2A);
        gfx.fill(0, HEADER_H + 7, this.width, HEADER_H + 8, 0xFF71B7FF);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 9, 0xFFFFFFFF);
        gfx.drawCenteredString(this.font,
                Component.translatable("screen.mimistweaks.macros.subtitle"),
                this.width / 2,
                22,
                0xFF9EC7F2);

        int listTop = getListTop();
        int listBottom = getListBottom();
        int listHeight = listBottom - listTop;
        int contentHeight = rows.size() * ROW_H;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        boolean needsScroll = contentHeight > listHeight;

        gfx.fill(0, this.height - FOOTER_H, this.width, this.height, 0xD9161E2A);
        gfx.fill(0, this.height - FOOTER_H, this.width, this.height - FOOTER_H + 1, 0xFF71B7FF);

        int contentLeft = Math.max(8, this.width / 2 - 560);
        int contentRight = Math.min(this.width - 8, this.width / 2 + 560);
        int usableRight = needsScroll ? contentRight - (SCROLLBAR_W + SCROLLBAR_GAP) : contentRight;
        gfx.fill(contentLeft, listTop - 8, contentRight, listBottom + 2, 0xAA1B2430);
        gfx.fill(contentLeft, listTop - 8, contentRight, listTop - 7, 0xFF2E3F54);
        gfx.fill(contentLeft, listBottom + 1, contentRight, listBottom + 2, 0xFF2E3F54);

        String macroCount = Component.translatable("screen.mimistweaks.macros.count", rows.size()).getString();
        gfx.drawString(this.font, macroCount, usableRight - this.font.width(macroCount) - 8, HEADER_H + 10, 0xFF8FB0D2, false);

        if (rows.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.mimistweaks.macros.empty"),
                    this.width / 2,
                    listTop + 18,
                    0xFFB8C2CE);
            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.mimistweaks.macros.empty_hint"),
                    this.width / 2,
                    listTop + 32,
                    0xFF8A98A9);
        }

        int keyW = 112;
        int toggleW = 70;
        int deleteW = 76;
        int gap = 8;
        int availableW = usableRight - contentLeft - 12;
        int textW = Math.max(180, Math.min(520, availableW - keyW - toggleW - deleteW - gap * 3));
        int usedW = textW + keyW + toggleW + deleteW + gap * 3;
        int rowStartX = contentLeft + 6 + Math.max(0, (availableW - usedW) / 2);
        int keyX = rowStartX + textW + gap;
        int toggleX = keyX + keyW + gap;
        int deleteX = toggleX + toggleW + gap;

        for (int i = 0; i < rows.size(); i++) {
            MacroRow row = rows.get(i);
            int y = listTop + i * ROW_H - scrollOffset;
            boolean visible = y >= listTop && y + ROW_H <= listBottom;

            row.setVisible(visible);
            if (!visible) {
                continue;
            }

            row.text.setX(rowStartX);
            row.text.setY(y + 4);
            row.text.setWidth(textW);

            row.keybind.setX(keyX);
            row.keybind.setY(y + 4);
            row.keybind.setWidth(keyW);

            row.enabled.setX(toggleX);
            row.enabled.setY(y + 4);
            row.enabled.setWidth(toggleW);

            row.delete.setX(deleteX);
            row.delete.setY(y + 4);
            row.delete.setWidth(deleteW);

            boolean hovered = mouseX >= contentLeft && mouseX <= usableRight && mouseY >= y + 2 && mouseY <= y + ROW_H - 2;
            int rowBg = hovered ? 0xE13A4A5F : 0xBC1E2733;
            int accent = row == capturingRow ? 0xFFFFB13B : (row.entry.isEnabled() ? 0xFF42F58D : 0xFF6A7688);
            gfx.fill(contentLeft, y + 2, usableRight, y + ROW_H - 2, rowBg);
            gfx.fill(contentLeft, y + 2, contentLeft + 2, y + ROW_H - 2, accent);

            gfx.fill(keyX - 1, y + 3, keyX + keyW + 1, y + 25, row == capturingRow ? 0xBB794500 : 0xAA1A3A62);
            gfx.fill(toggleX - 1, y + 3, toggleX + toggleW + 1, y + 25, row.entry.isEnabled() ? 0xBB1F6A42 : 0xBB4E2424);
            gfx.fill(deleteX - 1, y + 3, deleteX + deleteW + 1, y + 25, row.confirmDelete ? 0xCC8A3A00 : 0xCC8F2626);

            // Thin borders to make controls pop more from the row background.
            gfx.fill(keyX - 1, y + 3, keyX + keyW + 1, y + 4, 0xFF4F6B8C);
            gfx.fill(toggleX - 1, y + 3, toggleX + toggleW + 1, y + 4, row.entry.isEnabled() ? 0xFF43E68B : 0xFFCC4F4F);
            gfx.fill(deleteX - 1, y + 3, deleteX + deleteW + 1, y + 4, row.confirmDelete ? 0xFFFFB347 : 0xFFFF5A5A);
        }

        if (needsScroll) {
            int trackTop = listTop + 2;
            int trackBottom = listBottom - 2;
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(18, trackHeight * listHeight / Math.max(1, contentHeight));
            int thumbY = trackTop + (int) ((long) scrollOffset * (trackHeight - thumbHeight) / Math.max(1, maxScroll));
            int sbX = contentRight - SCROLLBAR_W - 2;

            gfx.fill(sbX, trackTop, sbX + SCROLLBAR_W, trackBottom, 0xCC233141);
            gfx.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbHeight, draggingScrollbar ? 0xFF6BC2FF : 0xFF4E9CDB);
            gfx.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + 1, 0xFFB6E5FF);
        }

        if (addButton != null) {
            boolean addHovered = mouseX >= addButton.getX() && mouseX <= addButton.getX() + addButton.getWidth()
                    && mouseY >= addButton.getY() && mouseY <= addButton.getY() + addButton.getHeight();
            gfx.fill(addButton.getX() - 1, addButton.getY() - 1, addButton.getX() + addButton.getWidth() + 1, addButton.getY() + addButton.getHeight() + 1,
                    addHovered ? 0xDD2FA85A : 0xAA1E4A1E);
        }
        if (doneButton != null) {
            boolean doneHovered = mouseX >= doneButton.getX() && mouseX <= doneButton.getX() + doneButton.getWidth()
                    && mouseY >= doneButton.getY() && mouseY <= doneButton.getY() + doneButton.getHeight();
            gfx.fill(doneButton.getX() - 1, doneButton.getY() - 1, doneButton.getX() + doneButton.getWidth() + 1, doneButton.getY() + doneButton.getHeight() + 1,
                    doneHovered ? 0xDD3C77C0 : 0xAA1E3B62);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingRow != null) {
            MacroRow row = capturingRow;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                row.entry.setKeyCode(TweaksClientSettings.KEY_UNBOUND);
            } else {
                row.entry.setKeyCode(keyCode);
            }
            capturingRow = null;
            row.refreshKeybindLabel();
            saveRows();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (capturingRow != null) {
            MacroRow row = capturingRow;
            row.entry.setKeyCode(TweaksClientSettings.encodeMouseButton(button));
            capturingRow = null;
            row.refreshKeybindLabel();
            saveRows();
            return true;
        }

        int listTop = getListTop();
        int listBottom = getListBottom();
        int listHeight = listBottom - listTop;
        int contentHeight = rows.size() * ROW_H;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        if (button == 0 && maxScroll > 0) {
            int contentRight = Math.min(this.width - 8, this.width / 2 + 560);
            int sbX = contentRight - SCROLLBAR_W - 2;
            int trackTop = listTop + 2;
            int trackBottom = listBottom - 2;
            if (mouseX >= sbX && mouseX <= sbX + SCROLLBAR_W && mouseY >= trackTop && mouseY <= trackBottom) {
                draggingScrollbar = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollOffset;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            int listTop = getListTop();
            int listBottom = getListBottom();
            int listHeight = listBottom - listTop;
            int contentHeight = rows.size() * ROW_H;
            int maxScroll = Math.max(0, contentHeight - listHeight);
            int trackHeight = (listBottom - 2) - (listTop + 2);
            int thumbHeight = Math.max(18, trackHeight * listHeight / Math.max(1, contentHeight));
            int moveRange = Math.max(1, trackHeight - thumbHeight);

            int delta = (int) mouseY - dragStartY;
            scrollOffset = (int) Math.round(dragStartScroll + (long) delta * maxScroll / moveRange);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listTop = getListTop();
        int listBottom = getListBottom();
        if (mouseY >= listTop && mouseY <= listBottom) {
            int listHeight = listBottom - listTop;
            int contentHeight = rows.size() * ROW_H;
            int maxScroll = Math.max(0, contentHeight - listHeight);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * ROW_H * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        saveRows();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void removeRow(MacroRow row) {
        row.setVisible(false);
        rows.remove(row);
        saveRows();
    }

    private void saveRows() {
        List<TweaksClientSettings.MacroEntry> values = new ArrayList<>();
        for (MacroRow row : rows) {
            values.add(row.entry.copy());
        }
        TweaksClientSettings.setMacros(values);
        ConfigPersistence.save();
    }

    private int getListTop() {
        return HEADER_H + 28;
    }

    private int getListBottom() {
        return this.height - FOOTER_H - 4;
    }

    // Legacy cleanup: only drop truly empty placeholder rows.
    private static boolean isPlaceholderMacro(TweaksClientSettings.MacroEntry macro) {
        return macro.getText().trim().isEmpty() && TweaksClientSettings.isUnboundKeyCode(macro.getKeyCode());
    }

    private final class MacroRow {
        private final TweaksClientSettings.MacroEntry entry;
        private final EditBox text;
        private final Button keybind;
        private final CycleButton<Boolean> enabled;
        private final Button delete;
        private boolean confirmDelete;

        private MacroRow(TweaksClientSettings.MacroEntry entry) {
            this.entry = entry;

            this.text = new EditBox(font, 0, 0, 120, 20,
                    Component.translatable("screen.mimistweaks.macros.text"));
            this.text.setValue(entry.getText());
            this.text.setHint(Component.translatable("screen.mimistweaks.macros.text_hint"));
            this.text.setResponder(value -> {
                this.entry.setText(value);
                resetDeleteConfirmation();
                saveRows();
            });

            this.keybind = Button.builder(Component.empty(), b -> {
                capturingRow = this;
                refreshKeybindLabel();
            }).bounds(0, 0, 88, 20).build();
            refreshKeybindLabel();

            this.enabled = CycleButton.<Boolean>builder(v ->
                            Component.literal(v ? "ON" : "OFF").withStyle(v ? ChatFormatting.GREEN : ChatFormatting.RED))
                    .withValues(false, true)
                    .withInitialValue(entry.isEnabled())
                    .create(0, 0, 70, 20, Component.empty(), (b, value) -> {
                        this.entry.setEnabled(value);
                        resetDeleteConfirmation();
                        saveRows();
                    });

            this.delete = Button.builder(Component.translatable("screen.mimistweaks.macros.btn.delete"), b -> {
                if (!confirmDelete) {
                    confirmDelete = true;
                    refreshDeleteButtonLabel();
                    return;
                }
                removeRow(this);
            }).bounds(0, 0, 58, 20).build();

            // Apply red/default destructive styling immediately when the row is created.
            refreshDeleteButtonLabel();
        }

        private void attach() {
            addRenderableWidget(text);
            addRenderableWidget(keybind);
            addRenderableWidget(enabled);
            addRenderableWidget(delete);
        }

        private void setVisible(boolean visible) {
            text.visible = visible;
            text.active = visible;
            keybind.visible = visible;
            keybind.active = visible;
            enabled.visible = visible;
            enabled.active = visible;
            delete.visible = visible;
            delete.active = visible;
        }

        private void refreshKeybindLabel() {
            if (capturingRow == this) {
                keybind.setMessage(Component.translatable("screen.mimistweaks.macros.keybind_capturing").withStyle(ChatFormatting.GOLD));
                return;
            }
            keybind.setMessage(Component.literal("[" + TweaksClientSettings.getKeybindDisplayName(entry.getKeyCode()) + "]").withStyle(ChatFormatting.AQUA));
        }

        private void resetDeleteConfirmation() {
            confirmDelete = false;
            refreshDeleteButtonLabel();
        }

        private void refreshDeleteButtonLabel() {
            if (delete != null) {
                delete.setMessage(
                        Component.translatable(confirmDelete ? "screen.mimistweaks.macros.btn.confirm_delete" : "screen.mimistweaks.macros.btn.delete")
                                .withStyle(confirmDelete ? ChatFormatting.GOLD : ChatFormatting.RED));
            }
        }
    }
}


