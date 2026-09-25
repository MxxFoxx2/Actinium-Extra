package jp.s12kuma01.actiniumextra.mixin.steady_debug_hud;

import jp.s12kuma01.actiniumextra.client.ActiniumExtraClientMod;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Throttles how often the F3 debug overlay text is rebuilt.
 * Port of embeddium-extra's steady_debug_hud.MixinDebugHud.
 * <p>
 * The vanilla debug screen recomputes every line each frame, which is comparatively expensive.
 * When enabled, this caches the left- and right-hand text lists and only rebuilds them on a fixed
 * interval (default 1 tick / 50 milliseconds), returning the cached lists in between to reduce CPU
 * usage. When the feature is off, the text is rebuilt every frame as in vanilla.
 * <p>
 * In 1.12.2 the text producers live on Minecraft's {@link GuiOverlayDebug}.
 */
@Mixin(GuiOverlayDebug.class)
public abstract class MixinForgeGuiDebugOverlay {

    @Unique
    private final List<String> actiniumExtra$leftTextCache = new ArrayList<>();
    @Unique
    private final List<String> actiniumExtra$rightTextCache = new ArrayList<>();
    @Unique
    private long actiniumExtra$nextUpdateNanos;
    @Unique
    private boolean actiniumExtra$rebuild = true;

    /**
     * Control when to rebuild debug text
     */
    @Inject(
            method = "renderDebugInfo",
            at = @At("HEAD")
    )
    private void actiniumExtra$beforeRenderDebugInfo(ScaledResolution resolution, CallbackInfo ci) {
        if (ActiniumExtraClientMod.options().extraSettings.steadyDebugHud) {
            long now = System.nanoTime();
            if (now >= this.actiniumExtra$nextUpdateNanos) {
                this.actiniumExtra$rebuild = true;
                this.actiniumExtra$nextUpdateNanos = now
                        + ActiniumExtraClientMod.options().extraSettings.steadyDebugHudRefreshInterval
                        * 50_000_000L;
            } else {
                this.actiniumExtra$rebuild = false;
            }
        } else {
            this.actiniumExtra$rebuild = true;
        }
    }

    /**
     * Cache left side debug text
     */
    @Inject(
            method = "call()Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actiniumExtra$beforeGetLeftDebugText(CallbackInfoReturnable<List<String>> cir) {
        if (!this.actiniumExtra$rebuild) {
            cir.setReturnValue(this.actiniumExtra$leftTextCache);
        }
    }

    @Inject(
            method = "call()Ljava/util/List;",
            at = @At("RETURN")
    )
    private void actiniumExtra$afterGetLeftDebugText(CallbackInfoReturnable<List<String>> cir) {
        if (this.actiniumExtra$rebuild) {
            this.actiniumExtra$leftTextCache.clear();
            this.actiniumExtra$leftTextCache.addAll(cir.getReturnValue());
        }
    }

    /**
     * Cache right side debug text
     */
    @Inject(
            method = "getDebugInfoRight()Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actiniumExtra$beforeGetRightDebugText(CallbackInfoReturnable<List<String>> cir) {
        if (!this.actiniumExtra$rebuild) {
            cir.setReturnValue(this.actiniumExtra$rightTextCache);
        }
    }

    @Inject(
            method = "getDebugInfoRight()Ljava/util/List;",
            at = @At("RETURN")
    )
    private void actiniumExtra$afterGetRightDebugText(CallbackInfoReturnable<List<String>> cir) {
        if (this.actiniumExtra$rebuild) {
            this.actiniumExtra$rightTextCache.clear();
            this.actiniumExtra$rightTextCache.addAll(cir.getReturnValue());
        }
    }
}
