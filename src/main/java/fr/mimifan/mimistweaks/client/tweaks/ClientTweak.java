package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;

public interface ClientTweak {
    TweaksClient.Tweak id();

    boolean isEnabled();

    void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc);

    default void onClientTick(Minecraft mc, LocalPlayer player) {}

    default void onMouseScroll(InputEvent.MouseScrollingEvent event, Minecraft mc, LocalPlayer player) {}

    default void onComputeFov(ComputeFovModifierEvent event) {}
}

