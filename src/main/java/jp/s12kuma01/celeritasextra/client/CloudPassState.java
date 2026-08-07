package jp.s12kuma01.celeritasextra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * Shared client render-thread state for the cloud rendering pass.
 * <p>
 * {@code EntityRenderer.renderCloudsCheck} installs its own projection and calls
 * {@code setupFog} before drawing clouds. We flag that window so the fog mixin can
 * push fog past the (extended) cloud volume only during the cloud pass, without
 * touching terrain fog. Client render thread only — no synchronization needed.
 */
public final class CloudPassState {

    /**
     * True while EntityRenderer.renderCloudsCheck is executing.
     */
    public static boolean inCloudPass = false;

    private CloudPassState() {
    }

    /**
     * Far distance (in blocks) that must be reached for clouds to render out to the
     * configured cloud distance. Covers the cloud mesh's far horizontal corner plus
     * the camera-to-cloud vertical distance and a small grid-snap margin.
     */
    public static float cloudFar(int cloudDistance) {
        Minecraft minecraft = Minecraft.getMinecraft();
        float cloudHeight = cloudHeight(128.0F);
        Entity viewEntity = minecraft.getRenderViewEntity();
        float verticalDistance = viewEntity == null
                ? Math.abs(cloudHeight)
                : (float) Math.abs(cloudHeight - viewEntity.posY);
        return MathHelper.SQRT_2 * cloudDistance * 32.0F + verticalDistance + 16.0F;
    }

    /**
     * Resolve the effective dimension cloud height, including this mod's configured override.
     */
    public static float cloudHeight(float fallback) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world != null && minecraft.world.provider != null) {
            return minecraft.world.provider.getCloudHeight();
        }
        return fallback;
    }
}
