package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Fullbright tweak — forces the light texture to full white via MixinLightTexture.
 * No Night Vision effect is used; the Mixin intercepts LightTexture.updateLightTexture()
 * every frame and overwrites every pixel with 0xFFFFFFFF.
 */
public final class FullbrightTweak implements ClientTweak {

    /** Volatile so the Mixin (called from the render thread) always sees the latest value. */
    private static volatile boolean fullbrightActive = false;

    private boolean enabled = false;

    /** Called by MixinLightTexture each render frame.
     *  Returns true when Fullbright is enabled OR when X-Ray is active
     *  (so X-Ray visible blocks are also fully lit). */
    public static boolean isFullbrightActive() {
        return fullbrightActive || XRayBlockList.isActive();
    }

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.FULLBRIGHT;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        this.enabled = enabled;
        fullbrightActive = enabled;
        // Mark the light texture dirty so the Mixin (or normal code) runs next frame.
        mc.execute(mc.levelRenderer::allChanged);
        player.displayClientMessage(
            Component.translatable(enabled
                ? "message.mimistweaks.fullbright.enabled"
                : "message.mimistweaks.fullbright.disabled"),
            true);
    }
}
