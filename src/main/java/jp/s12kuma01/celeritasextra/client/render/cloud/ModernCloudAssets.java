package jp.s12kuma01.celeritasextra.client.render.cloud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Runtime availability check for the Minecraft 1.21.6 cloud texture supplied by AssetMover.
 *
 * <p>The optional mod and the resource are checked separately: merely finding AssetMover is not
 * enough to enable Modern Clouds when its first-run download failed. Successful checks are cached;
 * failed checks are retried at a low rate so a resource that finishes appearing during startup can
 * unlock the option without restarting the options screen.</p>
 */
public final class  ModernCloudAssets {

    public static final ResourceLocation CLOUD_TEXTURE =
            new ResourceLocation("celeritasextra", "textures/environment/clouds_1_21_6.png");

    private static final long FAILED_CHECK_RETRY_NANOS = TimeUnit.SECONDS.toNanos(1L);

    private static boolean available;
    private static long lastFailedCheckNanos = Long.MIN_VALUE;

    private ModernCloudAssets() {
    }

    /** Returns whether AssetMover is installed and its acquired texture is currently readable. */
    public static boolean isAvailable() {
        if (!Loader.isModLoaded("assetmover")) {
            return false;
        }
        if (available) {
            return true;
        }

        long now = System.nanoTime();
        if (lastFailedCheckNanos != Long.MIN_VALUE
                && now - lastFailedCheckNanos < FAILED_CHECK_RETRY_NANOS) {
            return false;
        }

        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager == null) {
            lastFailedCheckNanos = now;
            return false;
        }

        try (IResource ignored = resourceManager.getResource(CLOUD_TEXTURE)) {
            available = true;
            return true;
        } catch (IOException | RuntimeException exception) {
            lastFailedCheckNanos = now;
            return false;
        }
    }

    /** Re-check after resource packs are reloaded or AssetMover finishes startup work. */
    public static void invalidate() {
        available = false;
        lastFailedCheckNanos = Long.MIN_VALUE;
    }
}
