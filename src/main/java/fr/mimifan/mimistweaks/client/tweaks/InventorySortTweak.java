package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper.SortMode;
import fr.mimifan.mimistweaks.client.tweaks.InventorySortHelper.SortOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class InventorySortTweak implements ClientTweak {

    private boolean enabled = true;
    /** Queue of [containerId, slotNetworkIndex, button] */
    private final Deque<int[]> pendingOps = new ArrayDeque<>();
    /** The containerId this sort run was started for – used to abort if the screen changes. */
    private int expectedContainerId = -1;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.SORT_INVENTORY;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        this.enabled = enabled;
        player.displayClientMessage(
            Component.translatable(enabled
                ? "message.mimistweaks.sort.enabled"
                : "message.mimistweaks.sort.disabled"),
            true);
    }

    /**
     * Schedule a sort of the player's main inventory (slots 9-44) using alphabetical order.
     */
    public void triggerPlayerSort(Minecraft mc, LocalPlayer player, SortMode mode, SortOrder order) {
        pendingOps.clear();
        AbstractContainerMenu menu = player.inventoryMenu;
        expectedContainerId = menu.containerId;
        scheduleSortOps(menu, 9, 45, mode, order);
    }

    /**
     * Schedule a sort of the first {@code containerSlotCount} slots of the given container menu.
     */
    public void triggerContainerSort(AbstractContainerMenu menu, int containerSlotCount,
                                     SortMode mode, SortOrder order) {
        pendingOps.clear();
        expectedContainerId = menu.containerId;
        scheduleSortOps(menu, 0, containerSlotCount, mode, order);
    }

    private void scheduleSortOps(AbstractContainerMenu menu, int start, int end,
                                  SortMode mode, SortOrder order) {
        int clampedEnd = Math.min(end, menu.slots.size());

        List<ItemStack> current = new ArrayList<>();
        for (int i = start; i < clampedEnd; i++) {
            current.add(menu.slots.get(i).getItem().copy());
        }

        ItemStack[] cur = current.toArray(new ItemStack[0]);
        int containerId = menu.containerId;

        // First compact identical stacks in-place to save space before reordering.
        compactStacksInPlace(cur, start, containerId);

        List<ItemStack> compacted = new ArrayList<>(cur.length);
        for (ItemStack stack : cur) {
            compacted.add(stack.copy());
        }
        List<ItemStack> sorted = InventorySortHelper.sortItems(compacted, mode, order);

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).isEmpty()) break;
            if (stackMatches(cur[i], sorted.get(i))) continue;

            // Find source slot j
            int j = -1;
            for (int k = i + 1; k < cur.length; k++) {
                if (stackMatches(cur[k], sorted.get(i))) {
                    j = k;
                    break;
                }
            }
            if (j == -1) continue;

            int slotI = start + i;
            int slotJ = start + j;

            if (cur[i].isEmpty()) {
                // Move j → i : 2 clicks (cursor was empty, no merge risk)
                pendingOps.add(new int[]{containerId, slotJ, 0});
                pendingOps.add(new int[]{containerId, slotI, 0});
            } else if (sameItemType(cur[i], cur[j])) {
                // Same item type → left-click would MERGE instead of swap.
                // Route through an empty temp slot to avoid merging.
                int tempSlot = findEmptySlot(cur, i + 1);
                if (tempSlot != -1) {
                    int slotTemp = start + tempSlot;
                    // Move i → temp (2 clicks)
                    pendingOps.add(new int[]{containerId, slotI,   0});
                    pendingOps.add(new int[]{containerId, slotTemp, 0});
                    // Move j → i (2 clicks; temp ensures slotJ is still untouched)
                    pendingOps.add(new int[]{containerId, slotJ,   0});
                    pendingOps.add(new int[]{containerId, slotI,   0});
                    // Move temp → j (2 clicks)
                    pendingOps.add(new int[]{containerId, slotTemp, 0});
                    pendingOps.add(new int[]{containerId, slotJ,    0});
                    // Update tracking array
                    ItemStack tmp = cur[i];
                    cur[i] = cur[j];
                    cur[j] = tmp;
                    // (tempSlot remains empty throughout)
                    continue;
                }
                // No empty temp slot available → skip this pair to avoid merging
            } else {
                // Different types: normal 3-click swap
                pendingOps.add(new int[]{containerId, slotI, 0});
                pendingOps.add(new int[]{containerId, slotJ, 0});
                pendingOps.add(new int[]{containerId, slotI, 0});
            }

            // Update tracking array
            ItemStack tmp = cur[i];
            cur[i] = cur[j];
            cur[j] = tmp;
        }
    }

    private void compactStacksInPlace(ItemStack[] cur, int start, int containerId) {
        for (int i = 0; i < cur.length; i++) {
            if (cur[i].isEmpty()) continue;

            int maxStackSize = cur[i].getMaxStackSize();
            if (cur[i].getCount() >= maxStackSize) continue;

            for (int j = i + 1; j < cur.length; j++) {
                if (!sameItemType(cur[i], cur[j])) continue;

                int free = maxStackSize - cur[i].getCount();
                if (free <= 0) break;

                int move = Math.min(free, cur[j].getCount());
                if (move <= 0) continue;

                int slotI = start + i;
                int slotJ = start + j;

                // Pick up stack J, merge into I, then put back leftovers in J if needed.
                pendingOps.add(new int[]{containerId, slotJ, 0});
                pendingOps.add(new int[]{containerId, slotI, 0});
                if (cur[j].getCount() > move) {
                    pendingOps.add(new int[]{containerId, slotJ, 0});
                }

                cur[i].grow(move);
                cur[j].shrink(move);
                if (cur[j].getCount() <= 0) {
                    cur[j] = ItemStack.EMPTY;
                }

                if (cur[i].getCount() >= maxStackSize) {
                    break;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean stackMatches(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b);
    }

    private static boolean sameItemType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(a, b);
    }

    /** Find the first empty slot in cur[] at index >= startFrom. Returns -1 if none. */
    private static int findEmptySlot(ItemStack[] cur, int startFrom) {
        for (int k = startFrom; k < cur.length; k++) {
            if (cur[k].isEmpty()) return k;
        }
        return -1;
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (pendingOps.isEmpty()) return;

        // Guard: abort if no container GUI is open anymore
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            pendingOps.clear();
            expectedContainerId = -1;
            return;
        }

        // Guard: abort if a different container was opened in the meantime
        if (containerScreen.getMenu().containerId != expectedContainerId) {
            pendingOps.clear();
            expectedContainerId = -1;
            return;
        }

        if (mc.gameMode == null) {
            return;
        }

        // Drain the whole queue in one tick so sorting feels instant.
        while (!pendingOps.isEmpty()) {
            int[] op = pendingOps.poll();
            mc.gameMode.handleInventoryMouseClick(op[0], op[1], op[2], ClickType.PICKUP, player);
        }
        expectedContainerId = -1;
    }
}

