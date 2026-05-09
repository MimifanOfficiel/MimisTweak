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
        ItemStack bestTool = findBestTool(player, blockState);

        if (bestTool.isEmpty()) {
            return;
        }

        int slotIndex = findHotbarSlotWithItem(player.getInventory(), bestTool);
        if (slotIndex < 0 || player.getInventory().selected == slotIndex) {
            return;
        }
        player.getInventory().selected = slotIndex;
    }

    /**
     * Finds the best tool for breaking the given block state.
     * Returns an ItemStack representing the best tool, or an empty ItemStack if no suitable tool is found.
     */
    private ItemStack findBestTool(LocalPlayer player, BlockState blockState) {
        Inventory inventory = player.getInventory();
        ItemStack bestTool = ItemStack.EMPTY;
        float bestSpeed = 1.0F;

        // Auto-select from hotbar only so the switch stays instant and side-effect free.
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }

            float mineSpeed = itemStack.getDestroySpeed(blockState);
            // Prefer tools that are actually effective for the targeted block.
            if (!itemStack.isCorrectToolForDrops(blockState) && mineSpeed <= 1.0F) {
                continue;
            }
            if (mineSpeed > bestSpeed) {
                bestSpeed = mineSpeed;
                bestTool = itemStack;
            }
        }

        return bestTool;
    }

    /**
     * Finds the slot index of an item in the inventory.
     * Returns -1 if not found.
     */
    private int findHotbarSlotWithItem(Inventory inventory, ItemStack targetItem) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(item, targetItem)) {
                return i;
            }
        }
        return -1;
    }
}

