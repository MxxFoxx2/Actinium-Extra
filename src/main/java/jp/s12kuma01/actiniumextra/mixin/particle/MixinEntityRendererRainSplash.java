package jp.s12kuma01.actiniumextra.mixin.particle;

import jp.s12kuma01.actiniumextra.client.ActiniumExtraClientMod;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the rain-splash particle pass when particles or rain splashes are disabled.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererRainSplash {

    @Inject(method = "addRainParticles()V", at = @At("HEAD"), cancellable = true)
    private void actiniumExtra$addRainParticles(CallbackInfo ci) {
        if (!ActiniumExtraClientMod.options().particleSettings.particles
                || !ActiniumExtraClientMod.options().particleSettings.rainSplash) {
            ci.cancel();
        }
    }
}
