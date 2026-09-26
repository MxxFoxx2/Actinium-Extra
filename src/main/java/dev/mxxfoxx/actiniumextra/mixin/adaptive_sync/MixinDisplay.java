package dev.mxxfoxx.actiniumextra.mixin.adaptive_sync;

import dev.mxxfoxx.actiniumextra.ActiniumExtraMod;
import dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod;
import dev.mxxfoxx.actiniumextra.client.gui.AdaptiveSyncSupport;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements adaptive VSync by intercepting Display.setVSyncEnabled()
 * Port of sodium-extra's adaptive_sync.MixinWindow
 * <p>
 * In 1.21.1: Window.updateVsync() with GLFW.glfwSwapInterval redirect
 * In 1.12.2 (Cleanroom): Display.setVSyncEnabled() from lwjglx compat layer
 * <p>
 * Adaptive VSync uses swap interval -1, which enables VSync when FPS is above
 * the monitor refresh rate and disables it when below, reducing stuttering.
 * Requires GLX_EXT_swap_control_tear (Linux) or WGL_EXT_swap_control_tear (Windows);
 * that probe lives in {@link AdaptiveSyncSupport} so this mixin, the option enum that offers
 * the mode, and the method that applies it can never disagree about the driver's capabilities.
 */
@Mixin(value = Display.class, remap = false)
public class MixinDisplay {

    @Inject(
            method = "setVSyncEnabled",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onSetVSyncEnabled(boolean enable, CallbackInfo ci) {
        if (ActiniumExtraClientMod.options().extraSettings.useAdaptiveSync && enable) {
            if (AdaptiveSyncSupport.isSupported()) {
                GLFW.glfwSwapInterval(-1);
                ci.cancel();
            } else {
                ActiniumExtraMod.LOGGER.warn("Adaptive VSync not supported, falling back to normal VSync.");
                ActiniumExtraClientMod.options().extraSettings.useAdaptiveSync = false;
                ActiniumExtraClientMod.options().writeChanges();
            }
        }
    }
}
