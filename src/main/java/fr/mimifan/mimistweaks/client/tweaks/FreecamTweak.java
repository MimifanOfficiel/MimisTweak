package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class FreecamTweak implements ClientTweak {

    private boolean enabled;

    private boolean prevMayfly;
    private boolean prevFlying;
    private boolean prevNoPhysics;
    private Entity prevCameraEntity;

    private double savedX, savedY, savedZ;
    private float savedYRot, savedXRot;

    private float yaw, pitch;

    private UUID ownerPlayerId;

    private FreecamEntity camera;

    @Override
    public TweaksClient.Tweak id() {
        return TweaksClient.Tweak.FREECAM;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, LocalPlayer player, Minecraft mc) {
        if (enabled) start(player, mc);
        else {
            if (canRestorePlayerState(player)) {
                stop(player, mc, "message.mimistweaks.freecam.disabled");
            } else {
                hardReset(mc, player);
            }
        }
    }

    public void onClientLifecycleTick(Minecraft mc) {
        if (!enabled) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || camera == null) {
            hardReset(mc, player);
            return;
        }

        if (!canRestorePlayerState(player)) {
            hardReset(mc, player);
        }
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled || camera == null) return;

        if (!canRestorePlayerState(player)) {
            hardReset(mc, player);
            return;
        }

        // Never send attack interactions while freecam is active.
        KeyMapping.set(mc.options.keyAttack.getKey(), false);

        if (TweaksClient.shouldStopForUnfocusedWindow(mc, TweaksClientSettings.isFreecamAllowWhenUnfocused())) {
            stop(player, mc, "message.mimistweaks.freecam.stopped_unfocused");
            return;
        }

        if (TweaksClient.shouldStopForOpenedScreen(mc, TweaksClientSettings.isFreecamStopOnOpenedScreen())) {
            stop(player, mc, "message.mimistweaks.freecam.stopped_gui");
            return;
        }

        // Convert mouse look into freecam rotation, then lock player view back in place.
        double prevCamX = camera.getX();
        double prevCamY = camera.getY();
        double prevCamZ = camera.getZ();
        float prevYaw = yaw;
        float prevPitch = pitch;

        float deltaYaw = Mth.wrapDegrees(player.getYRot() - savedYRot);
        float deltaPitch = player.getXRot() - savedXRot;
        yaw = Mth.wrapDegrees(yaw + deltaYaw);
        pitch = Mth.clamp(pitch + deltaPitch, -90.0f, 90.0f);
        player.setYRot(savedYRot);
        player.setXRot(savedXRot);
        player.yRotO = savedYRot;
        player.xRotO = savedXRot;
        player.setYHeadRot(savedYRot);
        player.yHeadRotO = savedYRot;
        player.setYBodyRot(savedYRot);
        player.yBodyRotO = savedYRot;

        float speed = 0.3f;
        if (mc.options.keySprint.isDown()) speed *= 2.5f;

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);

        double rx = -Math.cos(yawRad);
        double rz = -Math.sin(yawRad);

        double mx = 0, my = 0, mz = 0;

        if (mc.options.keyUp.isDown()) { mx += fx; my += fy; mz += fz; }
        if (mc.options.keyDown.isDown()) { mx -= fx; my -= fy; mz -= fz; }
        if (mc.options.keyLeft.isDown()) { mx -= rx; mz -= rz; }
        if (mc.options.keyRight.isDown()) { mx += rx; mz += rz; }
        if (mc.options.keyJump.isDown()) my += 1;
        if (mc.options.keyShift.isDown()) my -= 1;

        double len = Math.sqrt(mx*mx + my*my + mz*mz);
        if (len > 0) {
            mx = mx / len * speed;
            my = my / len * speed;
            mz = mz / len * speed;
        }

        double nx = camera.getX() + mx;
        double ny = camera.getY() + my;
        double nz = camera.getZ() + mz;

        camera.setPos(nx, ny, nz);
        camera.xo = prevCamX;
        camera.yo = prevCamY;
        camera.zo = prevCamZ;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = prevYaw;
        camera.xRotO = prevPitch;
        camera.setYHeadRot(yaw);
        camera.setYBodyRot(yaw);

        // Keep server-side player body fully frozen while freecam moves locally.
        player.setPos(savedX, savedY, savedZ);
        player.xo = savedX;
        player.yo = savedY;
        player.zo = savedZ;
        player.setDeltaMovement(Vec3.ZERO);
        player.input.forwardImpulse = 0;
        player.input.leftImpulse = 0;
        player.setSprinting(false);

        if (mc.getCameraEntity() != camera) {
            mc.setCameraEntity(camera);
        }
    }

    private void start(LocalPlayer player, Minecraft mc) {
        if (mc.level == null) {
            return;
        }

        enabled = true;
        ownerPlayerId = player.getUUID();

        prevMayfly = player.getAbilities().mayfly;
        prevFlying = player.getAbilities().flying;
        prevNoPhysics = player.noPhysics;
        prevCameraEntity = mc.getCameraEntity();

        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedYRot = player.getYRot();
        savedXRot = player.getXRot();

        yaw = savedYRot;
        pitch = savedXRot;

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.noPhysics = true;
        player.onUpdateAbilities();

        camera = new FreecamEntity(mc.level, savedX, savedY + player.getEyeHeight(), savedZ);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
        camera.xo = camera.getX();
        camera.yo = camera.getY();
        camera.zo = camera.getZ();

        mc.setCameraEntity(camera);
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        player.displayClientMessage(Component.translatable("message.mimistweaks.freecam.enabled"), true);
    }

    private void stop(LocalPlayer player, Minecraft mc, String key) {
        enabled = false;

        player.getAbilities().mayfly = prevMayfly;
        player.getAbilities().flying = prevFlying;
        player.noPhysics = prevNoPhysics;
        player.onUpdateAbilities();

        player.setPos(savedX, savedY, savedZ);
        player.setYRot(savedYRot);
        player.setXRot(savedXRot);

        mc.setCameraEntity(prevCameraEntity == null ? player : prevCameraEntity);
        camera = null;
        prevCameraEntity = null;
        ownerPlayerId = null;

        player.displayClientMessage(Component.translatable(key), true);
    }

    private boolean canRestorePlayerState(LocalPlayer player) {
        if (player == null || ownerPlayerId == null || camera == null) {
            return false;
        }
        return ownerPlayerId.equals(player.getUUID()) && camera.level() == player.level();
    }

    private void hardReset(Minecraft mc, LocalPlayer player) {
        enabled = false;

        if (player != null && mc.getCameraEntity() == camera) {
            mc.setCameraEntity(player);
        }

        camera = null;
        prevCameraEntity = null;
        ownerPlayerId = null;
    }

    // =========================
    // 🎥 ENTITÉ CAMÉRA PROPRE
    // =========================
    private static class FreecamEntity extends Entity {

        public FreecamEntity(Level level, double x, double y, double z) {
            super(EntityType.MARKER, level); // ultra léger
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setPos(x, y, z);
        }

        @Override
        public void tick() {}

        @Override
        protected void defineSynchedData() {}

        @Override
        protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}

        @Override
        protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
    }
}