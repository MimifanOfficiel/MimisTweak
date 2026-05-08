package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class XRayTweak implements ClientTweak {

    private boolean enabled = false;
    // Counter to delay second allChanged() call when enabling X-Ray
    private int delayCounter = 0;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.XRAY;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        this.enabled = enabled;
        XRayBlockList.setActive(enabled);
        // Force a full chunk re-render so the X-Ray changes take effect immediately.
        if (mc.levelRenderer != null) {
            mc.execute(mc.levelRenderer::allChanged);
            // Schedule a second allChanged() after 2 frames to let light texture update first
            if (enabled) {
                delayCounter = 2;
            }
        }
        player.displayClientMessage(
            Component.translatable(enabled
                ? "message.mimistweaks.xray.enabled"
                : "message.mimistweaks.xray.disabled"),
            true);
    }
    
    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (delayCounter > 0) {
            delayCounter--;
            if (delayCounter == 0 && mc.levelRenderer != null) {
                mc.execute(mc.levelRenderer::allChanged);
            }
        }
    }
}


