package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;

public final class ZoomTweak implements ClientTweak {
    private static final int MIN_ZOOM_PERCENT = 25;
    private static final int MAX_ZOOM_PERCENT = 120;

    private boolean held;
    private int holdZoomPercent;
    private float currentMultiplier = 1.0f;
    private float targetMultiplier = 1.0f;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.ZOOM;
    }

    @Override
    public boolean isEnabled() {
        return held;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        // Zoom works as hold only.
    }

    public void setHeld(boolean held) {
        if (held && !this.held) {
            // Start each hold from the configured base zoom, without keeping previous hold offset.
            this.holdZoomPercent = TweaksClientSettings.getZoomPercent();
        }
        this.held = held;
        this.targetMultiplier = held ? holdZoomPercent / 100.0f : 1.0f;
    }

    @Override
    public void onMouseScroll(InputEvent.MouseScrollingEvent event, Minecraft mc, LocalPlayer player) {
        if (!held) {
            return;
        }
        if (TweaksClient.shouldStopForOpenedScreen(mc, TweaksClientSettings.isZoomStopOnOpenedScreen())) {
            return;
        }
        if (TweaksClient.shouldStopForUnfocusedWindow(mc, TweaksClientSettings.isZoomAllowWhenUnfocused())) {
            return;
        }

        int step = TweaksClientSettings.getZoomScrollStepPercent();
        int updated = event.getScrollDeltaY() > 0
                ? holdZoomPercent - step
                : holdZoomPercent + step;
        holdZoomPercent = Math.max(MIN_ZOOM_PERCENT, Math.min(MAX_ZOOM_PERCENT, updated));
        targetMultiplier = holdZoomPercent / 100.0f;
        event.setCanceled(true);
    }

    @Override
    public void onComputeFov(ComputeFovModifierEvent event) {
        currentMultiplier += (targetMultiplier - currentMultiplier) * 0.20f;
        if (Math.abs(currentMultiplier - targetMultiplier) < 0.0005f) {
            currentMultiplier = targetMultiplier;
        }

        if (currentMultiplier < 0.9999f) {
            event.setNewFovModifier(event.getNewFovModifier() * currentMultiplier);
        }
    }
}

