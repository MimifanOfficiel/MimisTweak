package fr.mimifan.mimistweaks.client.tweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
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

    // -------------------------------------------------------------------------
    // Creative-tab category cache
    // -------------------------------------------------------------------------

    /** Ordered list of creative tabs – matches the creative search-tab display order. */
    private static final List<ResourceKey<CreativeModeTab>> TAB_ORDER = List.of(
            CreativeModeTabs.BUILDING_BLOCKS,
            CreativeModeTabs.COLORED_BLOCKS,
            CreativeModeTabs.NATURAL_BLOCKS,
            CreativeModeTabs.FUNCTIONAL_BLOCKS,
            CreativeModeTabs.REDSTONE_BLOCKS,
            CreativeModeTabs.TOOLS_AND_UTILITIES,
            CreativeModeTabs.COMBAT,
            CreativeModeTabs.FOOD_AND_DRINKS,
            CreativeModeTabs.INGREDIENTS,
            CreativeModeTabs.SPAWN_EGGS,
            CreativeModeTabs.OP_BLOCKS
    );

    private static final int MAX_ITEMS_PER_TAB = 10_000;

    /**
     * Cache: Item → (tabIndex * MAX_ITEMS_PER_TAB + positionInTab).
     * Built lazily on first sort; {@code null} means not yet built.
     */
    private static Map<Item, Integer> categoryCache = null;

    /** Call this to force a cache rebuild (e.g. after mods finish registering tabs). */
    public static void resetCategoryCache() {
        categoryCache = null;
    }

    private static Map<Item, Integer> getCategoryCache() {
        // If cache is empty, retry building: creative tab contents may not be ready on first call.
        if (categoryCache != null && !categoryCache.isEmpty()) return categoryCache;
        categoryCache = new HashMap<>();
        for (int t = 0; t < TAB_ORDER.size(); t++) {
            CreativeModeTab tab = getCreativeTab(TAB_ORDER.get(t));
            if (tab == null) continue;
            List<ItemStack> display = new ArrayList<>(tab.getDisplayItems());
            for (int p = 0; p < display.size(); p++) {
                categoryCache.putIfAbsent(display.get(p).getItem(), t * MAX_ITEMS_PER_TAB + p);
            }
        }
        return categoryCache;
    }

    private static CreativeModeTab getCreativeTab(ResourceKey<CreativeModeTab> key) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(key.location());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

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
                    .thenComparingInt(s -> BuiltInRegistries.ITEM.getId(s.getItem()));
        };

        if (order == SortOrder.DESCENDING) cmp = cmp.reversed();

        nonEmpty.sort(cmp);

        List<ItemStack> result = new ArrayList<>(nonEmpty);
        for (int i = 0; i < emptyCount; i++) result.add(ItemStack.EMPTY);
        return result;
    }

    private static int getCategoryOrdinal(ItemStack stack) {
        int unknownBase = TAB_ORDER.size() * MAX_ITEMS_PER_TAB;
        return getCategoryCache().getOrDefault(stack.getItem(), unknownBase + BuiltInRegistries.ITEM.getId(stack.getItem()));
    }

    /** Returns the creative-tab display name for the given stack (for tooltip/display purposes). */
    public static String getCategoryName(ItemStack stack) {
        Map<Item, Integer> cache = getCategoryCache();
        Integer key = cache.get(stack.getItem());
        if (key == null) return "Misc";
        int tabIdx = key / MAX_ITEMS_PER_TAB;
        if (tabIdx >= TAB_ORDER.size()) return "Misc";
        CreativeModeTab tab = getCreativeTab(TAB_ORDER.get(tabIdx));
        return tab != null ? tab.getDisplayName().getString() : "Misc";
    }
}
