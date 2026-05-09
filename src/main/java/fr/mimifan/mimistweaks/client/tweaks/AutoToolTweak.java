package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoToolTweak implements ClientTweak {
    private boolean enabled;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.AUTO_TOOL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        if (enabled) {
            this.enabled = true;
            player.displayClientMessage(Component.translatable("message.mimistweaks.autotool.enabled"), true);
        } else {
            this.enabled = false;
            player.displayClientMessage(Component.translatable("message.mimistweaks.autotool.disabled"), true);
        }
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled) {
            return;
        }

        // Only switch tool when the player is actually trying to break a block
        if (!mc.options.keyAttack.isDown()) {
            return;
        }

        // Get the block currently being looked at
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        if (!(hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (mc.level == null) {
            return;
        }

        BlockState blockState = mc.level.getBlockState(blockHit.getBlockPos());

        // 1. Search hotbar first
        int hotbarSlot = findBestToolSlot(player.getInventory(), blockState, 0, 9);
        if (hotbarSlot >= 0) {
            if (player.getInventory().selected != hotbarSlot) {
                player.getInventory().selected = hotbarSlot;
            }
            return;
        }

        // 2. If allowed, search main inventory (slots 9–35)
        if (!TweaksClientSettings.isAutoToolSearchInventory()) {
            return;
        }

        int mainSlot = findBestToolSlot(player.getInventory(), blockState, 9, 36);
        if (mainSlot < 0) {
            return;
        }

        // Move the item from the main inventory to the currently held slot by swapping
        // (equivalent to pressing a number key while hovering a slot in the inventory screen).
        // Container slot for inventory slots 9-35 equals the inventory slot index directly.
        mc.gameMode.handleInventoryMouseClick(
                player.containerMenu.containerId,
                mainSlot,
                player.getInventory().selected,
                ClickType.SWAP,
                player
        );
    }

    /**
     * Finds the slot index of the best tool for the given block state within [fromSlot, toSlot).
     * Returns -1 if no suitable tool is found.
     */
    private int findBestToolSlot(Inventory inventory, BlockState blockState, int fromSlot, int toSlot) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;

        for (int i = fromSlot; i < toSlot; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEmpty()) continue;
            float mineSpeed = itemStack.getDestroySpeed(blockState);
            if (!itemStack.isCorrectToolForDrops(blockState) && mineSpeed <= 1.0F) continue;
            if (mineSpeed > bestSpeed) {
                bestSpeed = mineSpeed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }
}

