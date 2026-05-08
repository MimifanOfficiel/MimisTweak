package fr.mimifan.mimistweaks.mixin;

import fr.mimifan.mimistweaks.client.tweaks.XRayBlockList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When X-Ray is active, make non-whitelisted blocks non-occluding so that their
 * neighbouring faces are not culled (i.e. you can "see through" them).
 * Also force visible X-Ray blocks to report maximum light emission so that
 * Ambient Occlusion is disabled for them (AO condition: getLightEmission() == 0).
 */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class MixinBlockStateBase {

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void xray$canOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (XRayBlockList.isActive()) {
            BlockState self = (BlockState) (Object) this;
            if (!XRayBlockList.isVisible(self.getBlock())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void xray$isSolidRender(BlockGetter level, BlockPos pos,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (XRayBlockList.isActive()) {
            BlockState self = (BlockState) (Object) this;
            if (!XRayBlockList.isVisible(self.getBlock())) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Force visible X-Ray blocks to report full light emission (15).
     * This disables Ambient Occlusion for those blocks in ModelBlockRenderer
     * (which checks: useAO = ... && state.getLightEmission() == 0 && ...).
     * Without this, AO bakes dark vertex colors even though the light texture is white.
     */
    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void xray$getLightEmission(CallbackInfoReturnable<Integer> cir) {
        if (XRayBlockList.isActive()) {
            BlockState self = (BlockState) (Object) this;
            if (XRayBlockList.isVisible(self.getBlock())) {
                cir.setReturnValue(15);
            }
        }
    }
}

