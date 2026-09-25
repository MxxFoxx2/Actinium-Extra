package jp.s12kuma01.actiniumextra.mixin.render.fog;

import jp.s12kuma01.actiniumextra.client.ActiniumExtraClientMod;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the distance-based void fog that darkens the view near the world bottom.
 * <p>
 * In 1.12.2 void fog color is applied inside {@code EntityRenderer.updateFogColor} from
 * {@link WorldProvider#getVoidFogYFactor()}. When void fog is disabled in the detail config this
 * redirect returns {@code 1.0}, which makes the game treat the player as fully above the void and
 * skip the effect; otherwise the real factor is used.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererVoidFog {

    @Redirect(
            method = "updateFogColor(F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProvider;getVoidFogYFactor()D")
    )
    private double actiniumExtra$toggleVoidFog(WorldProvider provider) {
        if (!ActiniumExtraClientMod.options().detailSettings.voidFog) {
            return 1.0D;
        }
        return provider.getVoidFogYFactor();
    }
}
