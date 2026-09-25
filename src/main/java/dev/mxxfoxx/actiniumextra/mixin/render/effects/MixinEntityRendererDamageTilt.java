package dev.mxxfoxx.actiniumextra.mixin.render.effects;

import dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the red camera tilt that vanilla applies while the player is hurt.
 * <p>
 * {@code EntityRenderer.hurtCameraEffect} rotates the camera by an amount driven by the hurt timer,
 * so disabling it cancels the effect at its head. Players turn this off for accessibility reasons or
 * to keep the view steady while taking damage; it changes nothing about the damage itself.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererDamageTilt {

    @Inject(
            method = "hurtCameraEffect(F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actiniumExtra$skipDamageTilt(float partialTicks, CallbackInfo ci) {
        if (!ActiniumExtraClientMod.options().renderSettings.damageTilt) {
            ci.cancel();
        }
    }
}
