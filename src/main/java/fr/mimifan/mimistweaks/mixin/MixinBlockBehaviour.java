package fr.mimifan.mimistweaks.mixin;

import fr.mimifan.mimistweaks.client.tweaks.XRayBlockList;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When X-Ray is active, make non-whitelisted blocks return RenderShape.INVISIBLE
 * so they are completely skipped by BlockRenderDispatcher (no geometry emitted).
 *
 * Targets BlockBehaviour.getRenderShape(BlockState) – vanilla signature, always stable.
 * Only affects blocks that do NOT override this method themselves (the vast majority).
 */
@Mixin(BlockBehaviour.class)
public class MixinBlockBehaviour {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void xray$getRenderShape(BlockState state,
                                      CallbackInfoReturnable<RenderShape> cir) {
        if (XRayBlockList.isActive() && !XRayBlockList.isVisible(state.getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}

