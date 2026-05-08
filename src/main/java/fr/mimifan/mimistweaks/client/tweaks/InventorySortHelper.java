package fr.mimifan.mimistweaks.client.tweaks;

import net.minecraft.world.item.*;

import java.util.*;

public final class InventorySortHelper {

    public enum SortMode {
        ALPHABETICAL, COUNT, CATEGORY;

        public SortMode next() {
            SortMode[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    public enum SortOrder {
        ASCENDING, DESCENDING;

        public SortOrder toggle() {
            return this == ASCENDING ? DESCENDING : ASCENDING;
        }
    }

    private InventorySortHelper() {}

    public static List<ItemStack> sortItems(List<ItemStack> items, SortMode mode, SortOrder order) {
        List<ItemStack> nonEmpty = new ArrayList<>();
        int emptyCount = 0;
        for (ItemStack s : items) {
            if (s.isEmpty()) emptyCount++;
            else nonEmpty.add(s.copy());
        }

        Comparator<ItemStack> cmp = switch (mode) {
            case ALPHABETICAL -> Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
            case COUNT        -> Comparator.comparingInt(ItemStack::getCount);
            case CATEGORY     -> Comparator.comparingInt(InventorySortHelper::getCategoryOrdinal)
                                          .thenComparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
        };

        if (order == SortOrder.DESCENDING) cmp = cmp.reversed();

        nonEmpty.sort(cmp);

        List<ItemStack> result = new ArrayList<>(nonEmpty);
        for (int i = 0; i < emptyCount; i++) result.add(ItemStack.EMPTY);
        return result;
    }

    private static int getCategoryOrdinal(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem) return 0;
        if (item.isEdible()) return 1;
        if (item instanceof SwordItem
                || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem
                || item instanceof ShieldItem) return 2;
        if (item instanceof ArmorItem) return 3;
        if (item instanceof TieredItem) return 4;
        return 5;
    }

    /** Returns a human-readable category name for tooltip/display purposes. */
    public static String getCategoryName(ItemStack stack) {
        return switch (getCategoryOrdinal(stack)) {
            case 0  -> "Blocks";
            case 1  -> "Food";
            case 2  -> "Combat";
            case 3  -> "Armor";
            case 4  -> "Tools";
            default -> "Misc";
        };
    }
}

