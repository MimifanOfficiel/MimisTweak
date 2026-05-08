package fr.mimifan.mimistweaks.client.tweaks;

import fr.mimifan.mimistweaks.client.TweaksClient;
import fr.mimifan.mimistweaks.utils.TweaksClientSettings;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FreecamTweak implements ClientTweak {

    private boolean enabled;

    private boolean prevMayfly;
    private boolean prevFlying;

    private double savedX, savedY, savedZ;
    private float savedYRot, savedXRot;

    private float yaw, pitch;

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
        else stop(player, mc, "message.mimistweaks.freecam.disabled");
    }

    @Override
    public void onClientTick(Minecraft mc, LocalPlayer player) {
        if (!enabled || camera == null) return;

        // =========================
        // 🎯 ROTATION = PLAYER (stable)
        // =========================
        yaw = player.getYRot();
        pitch = player.getXRot();

        // =========================
        // 🎮 INPUT
        // =========================
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

        if (player.input.up) { mx += fx; my += fy; mz += fz; }
        if (player.input.down) { mx -= fx; my -= fy; mz -= fz; }
        if (player.input.left) { mx -= rx; mz -= rz; }
        if (player.input.right) { mx += rx; mz += rz; }
        if (player.input.jumping) my += 1;
        if (player.input.shiftKeyDown) my -= 1;

        double len = Math.sqrt(mx*mx + my*my + mz*mz);
        if (len > 0) {
            mx = mx / len * speed;
            my = my / len * speed;
            mz = mz / len * speed;
        }

        double nx = camera.getX() + mx;
        double ny = camera.getY() + my;
        double nz = camera.getZ() + mz;

        // =========================
        // 🚀 POSITION FIX
        // =========================
        camera.setPos(nx, ny, nz);

        // 🔥 CRITIQUE (anti jitter)
        camera.xo = nx;
        camera.yo = ny;
        camera.zo = nz;

        camera.setYRot(yaw);
        camera.setXRot(pitch);

        // =========================
        // 🧊 FREEZE PLAYER (SANS rotation)
        // =========================
        player.setDeltaMovement(Vec3.ZERO);
        player.input.forwardImpulse = 0;
        player.input.leftImpulse = 0;
        player.setSprinting(false);

        // NE PAS reset rotation !!!

        if (mc.getCameraEntity() != camera) {
            mc.setCameraEntity(camera);
        }
    }

    private void start(LocalPlayer player, Minecraft mc) {
        enabled = true;

        prevMayfly = player.getAbilities().mayfly;
        prevFlying = player.getAbilities().flying;

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

        mc.setCameraEntity(camera);
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        player.displayClientMessage(Component.translatable("message.mimistweaks.freecam.enabled"), true);
    }

    private void stop(LocalPlayer player, Minecraft mc, String key) {
        enabled = false;

        player.getAbilities().mayfly = prevMayfly;
        player.getAbilities().flying = prevFlying;
        player.noPhysics = false;
        player.onUpdateAbilities();

        player.setPos(savedX, savedY, savedZ);
        player.setYRot(savedYRot);
        player.setXRot(savedXRot);

        mc.setCameraEntity(player);
        camera = null;

        player.displayClientMessage(Component.translatable(key), true);
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