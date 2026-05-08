package fr.mimifan.mimistweaks.mixin;

import fr.mimifan.mimistweaks.client.tweaks.XRayBlockList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When X-Ray is active, force block light calculation to return maximum brightness (15, 15)
 * for all visible X-Ray blocks. This ensures that when chunks are recompiled, the vertices
 * have lightmap UVs pointing to the bright white region of the light texture.
 *
 * Without this, blocks remain dark because their light data was baked at chunk compile time.
 */
@Mixin(LevelRenderer.class)
public class MixinBlockModelRenderer {

    /**
     * Intercepts getLightColor(BlockGetter, BlockPos) to return maximum light for X-Ray blocks.
     * This affects the lightmap UV coordinates baked into the vertex buffer during chunk compilation.
     */
    @Inject(method = "getLightColor*", at = @At("HEAD"), cancellable = true)
    private static void xray$getLightColor(BlockGetter level, BlockPos pos,
                                           CallbackInfoReturnable<Integer> cir) {
        if (XRayBlockList.isActive()) {
            var block = level.getBlockState(pos).getBlock();
            if (XRayBlockList.isVisible(block)) {
                // Return full light: (blockLight=15, skyLight=15) => 0xF000F0 (or similar encoding)
                // In Minecraft, getLightColor returns (blockLight << 4) | skyLight in the lower 8 bits,
                // plus sky light in upper bits. Max is: 0xF000F0 (15 << 4 | 15 << 20)
                cir.setReturnValue(0xF000F0);
            }
        }
    }
}

