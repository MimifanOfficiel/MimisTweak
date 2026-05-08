package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public final class AutoFishingTweak implements ClientTweak {
    private boolean enabled;
    private boolean waitingReelIn;
    private boolean waitingRecast;
    private int nextFishingActionTick;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.AUTO_FISHING;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        if (enabled) {
            start(player, mc);
        } else {
            stop(player, "message.mimistweaks.autofishing.disabled");
        }
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled) {
            return;
        }

        if (TweaksClient.shouldStopForUnfocusedWindow(mc, TweaksClientSettings.isFishingAllowWhenUnfocused())) {
            stop(player, "message.mimistweaks.autofishing.stopped_unfocused");
            return;
        }

        if (TweaksClient.shouldStopForOpenedScreen(mc, TweaksClientSettings.isFishingStopOnOpenedScreen())) {
            stop(player, "message.mimistweaks.autofishing.stopped_gui");
            return;
        }

        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            stop(player, "message.mimistweaks.autofishing.stopped_no_rod");
            return;
        }

        if (player.tickCount < nextFishingActionTick) {
            return;
        }

        if (waitingReelIn) {
            FishingHook hook = player.fishing;
            if (hook == null) {
                waitingReelIn = false;
                nextFishingActionTick = 0;
                return;
            }

            if (useMainHandRod(mc, player)) {
                waitingReelIn = false;
                waitingRecast = true;
                nextFishingActionTick = player.tickCount + TweaksClientSettings.computeRecastDelayTicks();
            } else {
                stop(player, "message.mimistweaks.autofishing.stopped_no_rod");
            }
            return;
        }

        if (waitingRecast) {
            if (useMainHandRod(mc, player)) {
                waitingRecast = false;
                nextFishingActionTick = player.tickCount + 8;
            } else {
                stop(player, "message.mimistweaks.autofishing.stopped_no_rod");
            }
            return;
        }

        FishingHook hook = player.fishing;
        if (hook != null && hook.getDeltaMovement().y < -TweaksClientSettings.getBiteDropThreshold()) {
            waitingReelIn = true;
            nextFishingActionTick = player.tickCount + TweaksClientSettings.computeReelInDelayTicks();
        }
    }

    private void start(LocalPlayer player, Minecraft mc) {
        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            player.displayClientMessage(Component.translatable("message.mimistweaks.autofishing.no_rod"), true);
            return;
        }

        enabled = true;
        waitingReelIn = false;
        waitingRecast = false;
        nextFishingActionTick = 0;
        player.displayClientMessage(Component.translatable("message.mimistweaks.autofishing.enabled"), true);

        if (!useMainHandRod(mc, player)) {
            stop(player, "message.mimistweaks.autofishing.stopped_no_rod");
        } else {
            nextFishingActionTick = player.tickCount + 8;
        }
    }

    private void stop(LocalPlayer player, String messageKey) {
        enabled = false;
        waitingReelIn = false;
        waitingRecast = false;
        nextFishingActionTick = 0;
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private static boolean useMainHandRod(Minecraft mc, LocalPlayer player) {
        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            return false;
        }
        assert mc.gameMode != null;
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        return true;
    }
}

