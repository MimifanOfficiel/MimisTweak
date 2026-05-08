package fr.mimifan.mimistweaks.screens;

import fr.mimifan.mimistweaks.client.tweaks.XRayBlockList;
import fr.mimifan.mimistweaks.utils.ConfigPersistence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class XRayBlockListScreen extends Screen {

    // ── Layout constants ────────────────────────────────────────────────────────
    private static final int ROW_H      = 20;
    private static final int HEADER_H   = 60;   // title + search box
    private static final int FOOTER_H   = 28;   // bottom buttons
    private static final int SCROLLBAR  = 6;

    // ── State ────────────────────────────────────────────────────────────────────
    private final Screen parent;

    /** All block entries (never changes after init). */
    private final List<BlockEntry> allEntries = new ArrayList<>();
    /** Filtered subset currently shown. */
    private final List<BlockEntry> visible    = new ArrayList<>();

    private EditBox searchBox;

    /** Pixel scroll offset (top of the list = 0). */
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int dragStartScroll;

    // ── Constructor ──────────────────────────────────────────────────────────────

    public XRayBlockListScreen(Screen parent) {
        super(Component.translatable("screen.mimistweaks.xray.title"));
        this.parent = parent;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        // Build block list once
        if (allEntries.isEmpty()) {
            BuiltInRegistries.BLOCK.forEach(block -> {
                if (block instanceof AirBlock) return;
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id == null) return;
                allEntries.add(new BlockEntry(block, id));
            });
            allEntries.sort(Comparator.comparing(e -> e.displayName));
        }

        // Search box
        searchBox = new EditBox(this.font, this.width / 2 - 110, 28, 220, 16,
                Component.translatable("screen.mimistweaks.xray.search"));
        searchBox.setHint(Component.translatable("screen.mimistweaks.xray.search"));
        searchBox.setResponder(s -> applyFilter(s));
        addWidget(searchBox);
        setFocused(searchBox);

        // Bottom buttons
        int bw = 90;
        int bh = 20;
        int totalBtnsW = bw * 4 + 6 * 3;
        int bx = this.width / 2 - totalBtnsW / 2;
        int by = this.height - FOOTER_H + 4;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.xray.btn.defaults"),
                b -> { XRayBlockList.resetToDefaults(); applyFilter(searchBox.getValue()); refreshChunks(); })
                .bounds(bx, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.xray.btn.clear"),
                b -> { XRayBlockList.clearAll(); applyFilter(searchBox.getValue()); refreshChunks(); })
                .bounds(bx + bw + 6, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.xray.btn.all"),
                b -> { XRayBlockList.selectAll(); applyFilter(searchBox.getValue()); refreshChunks(); })
                .bounds(bx + (bw + 6) * 2, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.mimistweaks.xray.btn.done"),
                b -> onClose())
                .bounds(bx + (bw + 6) * 3, by, bw, bh).build());

        applyFilter("");
    }

    private void applyFilter(String query) {
        visible.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        for (BlockEntry e : allEntries) {
            if (q.isEmpty() || e.displayName.toLowerCase(Locale.ROOT).contains(q)
                    || e.id.toString().contains(q)) {
                visible.add(e);
            }
        }
        scrollOffset = 0;
    }

    // ── Render ───────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);

        // Header background
        gfx.fill(0, 0, this.width, HEADER_H, 0xCC1A1E24);
        gfx.fill(0, HEADER_H - 1, this.width, HEADER_H, 0xFF71B7FF);

        // Title
        gfx.drawCenteredString(font, this.title, this.width / 2, 8, 0xFFFFFF);

        searchBox.render(gfx, mouseX, mouseY, partial);

        // Footer background
        gfx.fill(0, this.height - FOOTER_H, this.width, this.height, 0xCC1A1E24);
        gfx.fill(0, this.height - FOOTER_H, this.width, this.height - FOOTER_H + 1, 0xFF71B7FF);

        // List area
        int listTop    = HEADER_H + 2;
        int listBottom = this.height - FOOTER_H - 2;
        int listH      = listBottom - listTop;
        int contentH   = visible.size() * ROW_H;

        // Clamp scroll
        int maxScroll = Math.max(0, contentH - listH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Enable scissor to clip list to its area
        gfx.enableScissor(0, listTop, this.width, listBottom);

        int rowW  = this.width - (contentH > listH ? SCROLLBAR + 4 : 0) - 4;
        int rowX  = 2;
        int startRow = scrollOffset / ROW_H;
        int endRow   = Math.min(visible.size(), startRow + listH / ROW_H + 2);

        for (int i = startRow; i < endRow; i++) {
            BlockEntry entry = visible.get(i);
            int rowY = listTop + i * ROW_H - scrollOffset;
            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            // Row background
            boolean vis = XRayBlockList.isVisible(entry.id.toString());
            int bg = vis ? 0xCC1E3B1E : (hovered ? 0xCC2A2A2A : 0xCC1A1E24);
            gfx.fill(rowX, rowY, rowX + rowW, rowY + ROW_H - 1, bg);

            // Checkbox
            int checkX = rowX + 2;
            int checkY = rowY + (ROW_H - 10) / 2;
            gfx.fill(checkX, checkY, checkX + 10, checkY + 10, 0xFF888888);
            if (vis) {
                gfx.fill(checkX + 2, checkY + 2, checkX + 8, checkY + 8, 0xFF5AC85A);
            }

            // Block item icon (16×16)
            if (!entry.itemStack.isEmpty()) {
                gfx.renderItem(entry.itemStack, rowX + 15, rowY + 2);
            }

            // Block name
            String nameStr = entry.displayName;
            int textColor = vis ? 0xFF8FEA8F : 0xFFCCCCCC;
            gfx.drawString(font, nameStr, rowX + 34, rowY + 6, textColor, false);

            // Block ID (smaller, right-aligned or subtle)
            int idColor = 0xFF555577;
            String idStr = entry.id.toString();
            int maxIdW = rowW - 34 - font.width(nameStr) - 8;
            if (maxIdW > 20) {
                gfx.drawString(font, idStr, rowX + rowW - font.width(idStr) - 2, rowY + 6, idColor, false);
            }
        }

        gfx.disableScissor();

        // Scrollbar
        if (contentH > listH) {
            int sbX     = this.width - SCROLLBAR - 1;
            int sbTrack = listH;
            int sbH     = Math.max(16, sbTrack * listH / contentH);
            int sbY     = listTop + (int) ((long) scrollOffset * (sbTrack - sbH) / Math.max(1, contentH - listH));

            gfx.fill(sbX, listTop, sbX + SCROLLBAR, listBottom, 0xFF222222);
            gfx.fill(sbX, sbY, sbX + SCROLLBAR, sbY + sbH, 0xFF71B7FF);
        }

        // Counter
        String counter = visible.size() + " / " + allEntries.size() + " blocks";
        gfx.drawString(font, counter, this.width - font.width(counter) - 4, HEADER_H + 2, 0xFF888888, false);

        super.render(gfx, mouseX, mouseY, partial);
    }

    // ── Input ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int listTop    = HEADER_H + 2;
        int listBottom = this.height - FOOTER_H - 2;
        int listH      = listBottom - listTop;
        int contentH   = visible.size() * ROW_H;

        // Scrollbar drag start
        if (contentH > listH && button == 0) {
            int sbX = this.width - SCROLLBAR - 1;
            if (mx >= sbX && mx <= sbX + SCROLLBAR && my >= listTop && my <= listBottom) {
                draggingScrollbar = true;
                dragStartY      = (int) my;
                dragStartScroll = scrollOffset;
                return true;
            }
        }

        // Row click → toggle visibility
        if (button == 0 && mx >= 2 && my >= listTop && my < listBottom) {
            int rowIndex = (scrollOffset + (int) my - listTop) / ROW_H;
            if (rowIndex >= 0 && rowIndex < visible.size()) {
                BlockEntry entry = visible.get(rowIndex);
                String id = entry.id.toString();
                XRayBlockList.setVisible(id, !XRayBlockList.isVisible(id));
                if (XRayBlockList.isActive()) refreshChunks();
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            int listTop    = HEADER_H + 2;
            int listBottom = this.height - FOOTER_H - 2;
            int listH      = listBottom - listTop;
            int contentH   = visible.size() * ROW_H;
            int maxScroll  = Math.max(0, contentH - listH);
            int sbH        = Math.max(16, listH * listH / contentH);
            int trackH     = listH - sbH;

            if (trackH > 0) {
                int delta = (int) my - dragStartY;
                scrollOffset = (int) Math.round(dragStartScroll + (long) delta * maxScroll / trackH);
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingScrollbar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int listTop    = HEADER_H + 2;
        int listBottom = this.height - FOOTER_H - 2;
        if (my >= listTop && my <= listBottom) {
            int listH     = listBottom - listTop;
            int contentH  = visible.size() * ROW_H;
            int maxScroll = Math.max(0, contentH - listH);
            scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * ROW_H * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) return searchBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        ConfigPersistence.save();
        if (XRayBlockList.isActive()) refreshChunks();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private static void refreshChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.execute(mc.levelRenderer::allChanged);
    }

    // ── Entry ─────────────────────────────────────────────────────────────────────

    private static final class BlockEntry {
        final Block        block;
        final ResourceLocation id;
        final String       displayName;
        final ItemStack    itemStack;

        BlockEntry(Block block, ResourceLocation id) {
            this.block = block;
            this.id    = id;
            this.displayName = block.getName().getString();
            // Obtain item form for the icon
            net.minecraft.world.item.Item item = net.minecraft.world.item.Item.BY_BLOCK.get(block);
            this.itemStack = item != null ? new ItemStack(item) : ItemStack.EMPTY;
        }
    }
}

