package fr.mimifan.mimistweaks.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import fr.mimifan.mimistweaks.client.tweaks.FullbrightTweak;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When Fullbright or X-Ray is active (isFullbrightActive() covers both),
 * override the light texture with full white every frame so all blocks render at
 * maximum brightness regardless of their baked lightmap UV coordinates.
 */
@Mixin(LightTexture.class)
public class MixinLightTexture {

    @Shadow private DynamicTexture lightTexture;

    /** Dirty flag — we force it true so the original code also runs at least once on toggle. */
    @Shadow private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At("RETURN"))
    private void fb$forceBright(float partialTick, CallbackInfo ci) {
        if (!FullbrightTweak.isFullbrightActive()) return;

        NativeImage pixels = this.lightTexture.getPixels();
        if (pixels == null) return;

        for (int sky = 0; sky < 16; sky++)
            for (int block = 0; block < 16; block++)
                pixels.setPixelRGBA(block, sky, 0xFFFFFFFF);

        this.lightTexture.upload();
        // Keep the dirty flag set so this runs every frame while active.
        this.updateLightTexture = true;
    }
}
