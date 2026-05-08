package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class InventoryMouseTweaksTweak implements ClientTweak {

    private boolean enabled = true;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.MOUSE_TWEAKS;
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
                        ? "message.mimistweaks.mousetweaks.enabled"
                        : "message.mimistweaks.mousetweaks.disabled"),
                true);
    }
}

