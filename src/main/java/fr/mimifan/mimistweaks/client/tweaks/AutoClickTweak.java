package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class AutoClickTweak implements ClientTweak {
    private boolean enabled;
    private int nextAutoClickTick;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.AUTO_CLICK;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        if (enabled) {
            this.enabled = true;
            this.nextAutoClickTick = 0;
            player.displayClientMessage(Component.translatable("message.mimistweaks.autoclick.enabled"), true);
        } else {
            this.enabled = false;
            this.nextAutoClickTick = 0;
            // Release both keys in case continuous hold mode was active
            KeyMapping.set(mc.options.keyAttack.getKey(), false);
            KeyMapping.set(mc.options.keyUse.getKey(), false);
            player.displayClientMessage(Component.translatable("message.mimistweaks.autoclick.disabled"), true);
        }
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled) {
            return;
        }

        if (TweaksClient.shouldStopForUnfocusedWindow(mc, TweaksClientSettings.isAutoClickAllowWhenUnfocused())) {
            stop(player, mc, "message.mimistweaks.autoclick.stopped_unfocused");
            return;
        }

        if (TweaksClient.shouldStopForOpenedScreen(mc, TweaksClientSettings.isAutoClickStopOnOpenedScreen())) {
            stop(player, mc, "message.mimistweaks.autoclick.stopped_gui");
            return;
        }

        var key = TweaksClientSettings.isAutoClickRightClick()
                ? mc.options.keyUse.getKey()
                : mc.options.keyAttack.getKey();

        if (TweaksClientSettings.isAutoClickContinuous()) {
            // Simulate holding the mouse button down every tick
            KeyMapping.set(key, true);
            return;
        }

        // Release hold from any previous continuous session
        KeyMapping.set(mc.options.keyAttack.getKey(), false);
        KeyMapping.set(mc.options.keyUse.getKey(), false);

        if (player.tickCount < nextAutoClickTick) {
            return;
        }

        KeyMapping.click(key);
        nextAutoClickTick = player.tickCount + TweaksClientSettings.computeAutoClickDelayTicks();
    }

    private void stop(LocalPlayer player, Minecraft mc, String key) {
        enabled = false;
        nextAutoClickTick = 0;
        KeyMapping.set(mc.options.keyAttack.getKey(), false);
        KeyMapping.set(mc.options.keyUse.getKey(), false);
        player.displayClientMessage(Component.translatable(key), true);
    }
}
