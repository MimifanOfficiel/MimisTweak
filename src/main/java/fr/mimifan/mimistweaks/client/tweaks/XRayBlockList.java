package fr.mimifan.mimistweaks.client.tweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the runtime X-Ray state: active flag + set of visible block IDs.
 * All reads are thread-safe (chunk compilation runs off-thread).
 */
public final class XRayBlockList {

    /** Default block IDs shown through X-Ray (all overworld/nether ores). */
    public static final List<String> DEFAULT_ORE_IDS = List.of(
            "minecraft:coal_ore",             "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",             "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",           "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",             "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore",         "minecraft:deepslate_redstone_ore",
            "minecraft:diamond_ore",          "minecraft:deepslate_diamond_ore",
            "minecraft:lapis_ore",            "minecraft:deepslate_lapis_ore",
            "minecraft:emerald_ore",          "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore",      "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris",
            // Bonus: spawner, chest, barrel so you can also see structures
            "minecraft:chest",                "minecraft:trapped_chest",
            "minecraft:ender_chest"
    );

    private static volatile boolean active = false;
    private static final Set<String> visibleIds = ConcurrentHashMap.newKeySet();

    static {
        visibleIds.addAll(DEFAULT_ORE_IDS);
    }

    private XRayBlockList() {}

    // ── State ──────────────────────────────────────────────────────────────────

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean a) {
        active = a;
    }

    // ── Block visibility ───────────────────────────────────────────────────────

    public static boolean isVisible(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && visibleIds.contains(id.toString());
    }

    public static boolean isVisible(String blockId) {
        return visibleIds.contains(blockId);
    }

    public static void setVisible(String blockId, boolean visible) {
        if (visible) visibleIds.add(blockId);
        else          visibleIds.remove(blockId);
    }

    public static Set<String> getVisibleIds() {
        return Collections.unmodifiableSet(visibleIds);
    }

    // ── Presets ────────────────────────────────────────────────────────────────

    public static void resetToDefaults() {
        visibleIds.clear();
        visibleIds.addAll(DEFAULT_ORE_IDS);
    }

    public static void clearAll() {
        visibleIds.clear();
    }

    public static void selectAll() {
        BuiltInRegistries.BLOCK.forEach(b -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (id != null) visibleIds.add(id.toString());
        });
    }
}

