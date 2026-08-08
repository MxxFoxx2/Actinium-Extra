package jp.s12kuma01.celeritasextra.mixin.render.sky;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import jp.s12kuma01.celeritasextra.client.CeleritasExtraClientMod;
import jp.s12kuma01.celeritasextra.client.CloudPassState;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions.RenderSettings;
import jp.s12kuma01.celeritasextra.client.render.cloud.ModernCloudAssets;
import jp.s12kuma01.celeritasextra.client.render.cloud.ModernCloudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.CloudRenderer;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.VanillaResourceType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Controls cloud render distance and cloud scale via Forge's CloudRenderer and optionally delegates
 * its default-dimension cloud path to Celeritas Extra's modern renderer.
 * <p>
 * Cloud Distance: Two @Redirects decouple the dirty-check from geometry extent.
 * Cloud Scale: @ModifyReturnValue on getScale() (XZ tiling scale).
 * <p>
 * Scale values use quarter steps from 1-16 (default 4):
 * <ul>
 *   <li>1 = 0.25x (smallest clouds)</li>
 *   <li>4 = 1.0x (vanilla, default)</li>
 *   <li>16 = 4.0x (largest clouds)</li>
 * </ul>
 */
@Mixin(value = CloudRenderer.class, remap = false)
public class MixinCloudRenderer {

    @Shadow
    private int renderDistance;

    @Shadow
    private void dispose() {
    }

    @Unique
    private int celeritasExtra$prevCloudDist = -1;

    @Unique
    private int celeritasExtra$prevCloudScale = -1;

    @Unique
    private int celeritasExtra$prevModernClouds = -1;

    @Unique
    private ModernCloudRenderer celeritasExtra$modernCloudRenderer;

    // ========================
    // Cloud Distance
    // ========================

    /**
     * Detect cloud setting changes and force both renderers to rebuild their geometry.
     */
    @Inject(method = "checkSettings", at = @At("HEAD"))
    private void celeritasExtra$onCheckSettings(CallbackInfo ci) {
        var options = CeleritasExtraClientMod.options().renderSettings;
        int cloudDist = options.cloudDistance;
        int cloudScale = options.cloudScale;
        int modernClouds = options.modernClouds ? 1 : 0;
        if (cloudDist != celeritasExtra$prevCloudDist
                || cloudScale != celeritasExtra$prevCloudScale
                || modernClouds != celeritasExtra$prevModernClouds) {
            dispose();
            celeritasExtra$prevCloudDist = cloudDist;
            celeritasExtra$prevCloudScale = cloudScale;
            celeritasExtra$prevModernClouds = modernClouds;
        }
    }

    /**
     * Replace only Forge's default cloud renderer. A custom WorldProvider cloud renderer bypasses
     * this class entirely, while any unsupported modern path returns false and continues here.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void celeritasExtra$renderModernClouds(int cloudTicks, float partialTicks,
                                                   CallbackInfoReturnable<Boolean> cir) {
        RenderSettings settings = CeleritasExtraClientMod.options().renderSettings;
        if (!CloudPassState.usesModernCloudRenderer(settings)) {
            return;
        }

        if (celeritasExtra$modernCloudRenderer == null) {
            celeritasExtra$modernCloudRenderer = new ModernCloudRenderer();
        }
        if (celeritasExtra$modernCloudRenderer.render(cloudTicks, partialTicks, settings)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void celeritasExtra$reloadModernCloudTexture(IResourceManager resourceManager,
                                                         Predicate<IResourceType> resourcePredicate,
                                                         CallbackInfo ci) {
        if (resourcePredicate.test(VanillaResourceType.TEXTURES)) {
            ModernCloudAssets.invalidate();
            if (celeritasExtra$modernCloudRenderer != null) {
                celeritasExtra$modernCloudRenderer.onTextureReload();
            }
        }
    }

    @Inject(method = "dispose", at = @At("TAIL"))
    private void celeritasExtra$disposeModernClouds(CallbackInfo ci) {
        if (celeritasExtra$modernCloudRenderer != null) {
            celeritasExtra$modernCloudRenderer.destroy();
            celeritasExtra$modernCloudRenderer = null;
        }
    }

    /**
     * Bypass the dirty-check when a custom cloud distance is active.
     */
    @Redirect(
            method = "checkSettings",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraftforge/client/CloudRenderer;renderDistance:I",
                    opcode = Opcodes.GETFIELD)
    )
    private int celeritasExtra$redirectDirtyCheck(CloudRenderer self) {
        int cloudDist = CeleritasExtraClientMod.options().renderSettings.cloudDistance;
        if (cloudDist > 0) {
            return Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
        }
        return this.renderDistance;
    }

    /**
     * Override the render distance used for cloud geometry extent.
     */
    @Redirect(
            method = "vertices",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraftforge/client/CloudRenderer;renderDistance:I",
                    opcode = Opcodes.GETFIELD)
    )
    private int celeritasExtra$redirectCloudDistance(CloudRenderer self) {
        int cloudDist = CeleritasExtraClientMod.options().renderSettings.cloudDistance;
        return cloudDist > 0 ? cloudDist : this.renderDistance;
    }

    // ========================
    // Cloud Scale
    // ========================

    /**
     * Apply the cloud scale multiplier (cloudScale / 4.0, so 4 = vanilla 1.0x) on top of
     * the vanilla base scale returned by getScale(). Non-destructive: preserves the vanilla
     * cloudMode logic and any other mod's modifications, unlike an @Overwrite.
     */
    @ModifyReturnValue(method = "getScale", at = @At("RETURN"))
    private int celeritasExtra$scaleClouds(int original) {
        int cloudScale = CeleritasExtraClientMod.options().renderSettings.cloudScale;
        return Math.max(1, original * cloudScale / RenderSettings.CLOUD_SCALE_VANILLA);
    }
}
