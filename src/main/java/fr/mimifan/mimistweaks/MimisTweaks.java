package fr.mimifan.mimistweaks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;

@Mod(MimisTweaks.MODID)
public class MimisTweaks {

    public static final String MODID = "mimistweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MimisTweaks(IEventBus modEventBus) {
        // Client-only mod — no server-side registration needed
    }

    @EventBusSubscriber(modid = MimisTweaks.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}



