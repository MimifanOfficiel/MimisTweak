package fr.mimifan.mimistweaks.client;

import fr.mimifan.mimistweaks.MimisTweaks;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MimisTweaks.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MimisKeybinds {
	private static final String CATEGORY = "key.categories.mimistweaks";

	private static final KeyMapping OPEN_CONFIG =
			new KeyMapping("key.mimistweaks.config", GLFW.GLFW_KEY_P, CATEGORY);

	private MimisKeybinds() {}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(OPEN_CONFIG);
	}

	public static int getConfigKeyCode() {
		return OPEN_CONFIG.getKey().getValue();
	}
}

