package fr.mimifan.mimistweaks.client.tweaks;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class TargetInfoTweak implements ClientTweak {
    private static final String HEART_FULL = "\u2665";
    private static final String HEART_EMPTY = "\u2661";
    private static final int ICON_SIZE = 16;
    private static final int ENTITY_PREVIEW_SIZE = 13;
    private static final int HOTBAR_CLEARANCE = 58;

    private boolean enabled = TweaksClientSettings.isTargetInfoEnabled();
    private TargetSnapshot current;

    public void syncEnabledFromSettings() {
        this.enabled = TweaksClientSettings.isTargetInfoEnabled();
        if (!enabled) {
            current = null;
        }
    }

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.TARGET_INFO;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        this.enabled = enabled;
        TweaksClientSettings.setTargetInfoEnabled(enabled);
        if (!enabled) {
            current = null;
        }
        player.displayClientMessage(Component.translatable(enabled
                ? "message.mimistweaks.targetinfo.enabled"
                : "message.mimistweaks.targetinfo.disabled"), true);
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled || mc.level == null) {
            current = null;
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            current = null;
            return;
        }

        if (hitResult instanceof EntityHitResult entityHit) {
            current = fromEntity(entityHit.getEntity());
            return;
        }

        if (hitResult instanceof BlockHitResult blockHit) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            current = fromBlock(state.getBlock());
            return;
        }

        current = null;
    }

    public void onRenderGui(GuiGraphics guiGraphics, Minecraft mc) {
        if (!enabled || current == null || mc.screen != null) {
            return;
        }

        Font font = mc.font;
        float scale = TweaksClientSettings.getTargetInfoScalePercent() / 100.0F;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int canvasW = Mth.floor(screenW / scale);
        int canvasH = Mth.floor(screenH / scale);

        List<LineSpec> lines = buildLines(current);
        if (lines.isEmpty()) {
            return;
        }

        int contentWidth = 0;
        for (LineSpec line : lines) {
            contentWidth = Math.max(contentWidth, font.width(line.text()));
        }

        boolean renderEquipment = current.playerEquipment != null && TweaksClientSettings.isTargetInfoShowPlayerEquipment();
        int iconRowHeight = renderEquipment ? 20 : 0;
        int bodyLeft = current.previewEntity != null || !current.iconStack.isEmpty() ? 28 : 8;
        int boxWidth = Math.max(84, contentWidth + bodyLeft + 6);
        int boxHeight = 8 + lines.size() * 10 + iconRowHeight;

        int x = computeX(canvasW, boxWidth);
        int y = computeY(canvasH, boxHeight);
        x = Mth.clamp(x, 4, Math.max(4, canvasW - boxWidth - 4));
        y = Mth.clamp(y, 4, Math.max(4, canvasH - boxHeight - 4));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);

        if (TweaksClientSettings.isTargetInfoShowBackground()) {
            drawPanel(guiGraphics, x, y, boxWidth, boxHeight);
        }

        if (current.previewEntity != null) {
            guiGraphics.fill(x + 6, y + 4, x + 6 + ICON_SIZE, y + 4 + ICON_SIZE, themedIconSlotColor());
            renderEntityPreview(guiGraphics, current.previewEntity, x + 14, y + 18);
        } else if (!current.iconStack.isEmpty()) {
            guiGraphics.fill(x + 6, y + 4, x + 6 + ICON_SIZE, y + 4 + ICON_SIZE, themedIconSlotColor());
            guiGraphics.renderItem(current.iconStack, x + 6, y + 4);
            guiGraphics.renderItemDecorations(font, current.iconStack, x + 6, y + 4);
        }

        int textX = x + bodyLeft;
        int textY = y + 4;
        for (LineSpec line : lines) {
            guiGraphics.drawString(font, line.text(), textX, textY, line.color(), false);
            textY += 10;
        }

        if (renderEquipment) {
            int iconX = textX;
            int iconY = y + boxHeight - 18;
            renderItemRow(guiGraphics, mc, current.playerEquipment, iconX, iconY);
        }

        guiGraphics.pose().popPose();
    }

    private int computeX(int canvasWidth, int boxWidth) {
        int offsetX = TweaksClientSettings.getTargetInfoOffsetX();
        return switch (TweaksClientSettings.getTargetInfoAnchor()) {
            case TOP_LEFT, BOTTOM_LEFT -> 6 + offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> canvasWidth - boxWidth - 6 + offsetX;
            case CENTER_TOP, CENTER_BOTTOM -> (canvasWidth - boxWidth) / 2 + offsetX;
            case CROSSHAIR -> canvasWidth / 2 + 14 + offsetX;
        };
    }

    private int computeY(int canvasHeight, int boxHeight) {
        int offsetY = TweaksClientSettings.getTargetInfoOffsetY();
        return switch (TweaksClientSettings.getTargetInfoAnchor()) {
            case TOP_LEFT, TOP_RIGHT -> 6 + offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> canvasHeight - boxHeight - 6 + offsetY;
            case CENTER_TOP -> 16 + offsetY;
            case CENTER_BOTTOM -> canvasHeight - boxHeight - HOTBAR_CLEARANCE + offsetY;
            case CROSSHAIR -> canvasHeight / 2 + 8 + offsetY;
        };
    }

    private static List<LineSpec> buildLines(TargetSnapshot snapshot) {
        List<LineSpec> lines = new ArrayList<>();
        lines.add(new LineSpec(snapshot.name, themedTitleTextColor()));
        if (TweaksClientSettings.isTargetInfoShowModName()) {
            lines.add(new LineSpec(snapshot.modName, themedModTextColor()));
        }

        if (snapshot.health != null && TweaksClientSettings.isTargetInfoShowHealth()) {
            lines.add(new LineSpec(snapshot.health, themedHealthTextColor()));
        }
        return lines;
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        guiGraphics.fill(x - 1, y - 1, right + 1, bottom + 1, themedOuterBorderColor());
        guiGraphics.fill(x, y, right, bottom, themedBackgroundColor());
        guiGraphics.fill(x, y, right, y + 1, themedHighlightColor());
        guiGraphics.fill(x, bottom - 1, right, bottom, themedInnerBorderColor());
        guiGraphics.fill(x, y, x + 1, bottom, themedInnerBorderColor());
        guiGraphics.fill(right - 1, y, right, bottom, themedInnerBorderColor());
        guiGraphics.fill(x + 1, y + 1, right - 1, y + 2, themedAccentColor());
    }

    private static void renderEntityPreview(GuiGraphics guiGraphics, LivingEntity entity, int centerX, int bottomY) {
        // Jade-like tiny 3D preview of the targeted entity.
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                guiGraphics,
                centerX - 8,
                bottomY - 16,
                centerX + 8,
                bottomY,
                ENTITY_PREVIEW_SIZE,
                0.0F,
                0.0F,
                0.0F,
                entity);
    }

    private static void renderItemRow(GuiGraphics guiGraphics, Minecraft mc, List<ItemStack> stacks, int x, int y) {
        RenderSystem.enableDepthTest();
        int idx = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                idx++;
                continue;
            }
            int drawX = x + idx * 20;
            guiGraphics.renderItem(stack, drawX, y);
            guiGraphics.renderItemDecorations(mc.font, stack, drawX, y);
            idx++;
        }
    }

    private static int themedBackgroundColor() {
        int alpha = TweaksClientSettings.getTargetInfoBackgroundOpacity() << 24;
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> alpha | 0x101217;
            case LIGHT -> alpha | 0xF2F2F2;
            case ACCENT -> alpha | 0x1A2E45;
        };
    }

    private static int themedTextColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK, ACCENT -> 0xFFFFFF;
            case LIGHT -> 0x202020;
        };
    }

    private static int themedTitleTextColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK, ACCENT -> 0xFFFFFF;
            case LIGHT -> 0x1D2633;
        };
    }

    private static int themedModTextColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0x6C7DFF;
            case LIGHT -> 0x4D5EE0;
            case ACCENT -> 0x8BA2FF;
        };
    }

    private static int themedHealthTextColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK, ACCENT -> 0xFF5A5A;
            case LIGHT -> 0xD22727;
        };
    }

    private static int themedAccentColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0xFF8397B6;
            case LIGHT -> 0xFF93A8C2;
            case ACCENT -> 0xFF8FB7E8;
        };
    }

    private static int themedInnerBorderColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0xFF3A4556;
            case LIGHT -> 0xFF6A7688;
            case ACCENT -> 0xFF597CA6;
        };
    }

    private static int themedOuterBorderColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0xFF05070B;
            case LIGHT -> 0xFF303846;
            case ACCENT -> 0xFF111A26;
        };
    }

    private static int themedHighlightColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0xFFB7C6D8;
            case LIGHT -> 0xFFD9E2EE;
            case ACCENT -> 0xFFBDD8F4;
        };
    }

    private static int themedIconSlotColor() {
        return switch (TweaksClientSettings.getTargetInfoTheme()) {
            case DARK -> 0xA0222730;
            case LIGHT -> 0xA0CCD5DF;
            case ACCENT -> 0xA0263E58;
        };
    }

    private static TargetSnapshot fromBlock(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String namespace = key.getNamespace();
        String modName = resolveModName(namespace);
        ItemStack icon = block.asItem().getDefaultInstance();
        return new TargetSnapshot(block.getName().getString(), modName, null, null, icon, null);
    }

    private static TargetSnapshot fromEntity(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String namespace = key.getNamespace();
        String modName = resolveModName(namespace);

        String healthLine = null;
        if (entity instanceof LivingEntity living) {
            healthLine = formatHearts(living.getHealth(), living.getMaxHealth());
        }

        List<ItemStack> equipment = null;
        if (entity instanceof Player player) {
            equipment = new ArrayList<>();
            equipment.add(player.getMainHandItem().copy());
            equipment.add(player.getOffhandItem().copy());
            // Helmet -> boots order for readability.
            List<ItemStack> armor = player.getInventory().armor;
            for (int i = armor.size() - 1; i >= 0; i--) {
                equipment.add(armor.get(i).copy());
            }
        }

        return new TargetSnapshot(entity.getDisplayName().getString(), modName, healthLine, equipment, ItemStack.EMPTY,
                entity instanceof LivingEntity living ? living : null);
    }

    private static String resolveModName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    private static String formatHearts(float health, float maxHealth) {
        float clampedHealth = Mth.clamp(health, 0.0F, Math.max(maxHealth, 1.0F));
        int heartSlots = Mth.clamp(Mth.ceil(maxHealth / 2.0F), 1, 20);
        int fullHearts = Mth.floor(clampedHealth / 2.0F);
        boolean halfHeart = ((int) Math.ceil(clampedHealth)) % 2 == 1 && fullHearts < heartSlots;

        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < fullHearts; i++) {
            hearts.append(HEART_FULL);
        }
        if (halfHeart) {
            hearts.append(HEART_EMPTY);
        }
        int emptyCount = heartSlots - fullHearts - (halfHeart ? 1 : 0);
        for (int i = 0; i < emptyCount; i++) {
            hearts.append(HEART_EMPTY);
        }

        return hearts + " (" + String.format("%.1f", clampedHealth) + "/" + String.format("%.1f", maxHealth) + ")";
    }

    private record LineSpec(String text, int color) {
    }

    private record TargetSnapshot(String name, String modName, String health, List<ItemStack> playerEquipment,
                                  ItemStack iconStack, LivingEntity previewEntity) {
    }
}


