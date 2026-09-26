package dev.mxxfoxx.actiniumextra.client.gui;

import org.lwjgl.glfw.GLFW;

/**
 * Runtime probe for the swap-control tear extensions adaptive VSync is built on.
 * <p>
 * Adaptive VSync is presented with a swap interval of {@code -1}, which only does anything on a
 * driver exposing {@code GLX_EXT_swap_control_tear} (GLX/Linux) or {@code WGL_EXT_swap_control_tear}
 * (WGL/Windows). The same question is asked from three places: the option enum that decides which
 * VSync modes to offer, the method that applies the chosen mode, and the {@code Display} mixin that
 * intercepts {@code setVSyncEnabled}. They live in two different packages and must never disagree,
 * so the predicate lives here and is asked once per call rather than being spelled out three times.
 * <p>
 * The result is deliberately NOT memoized in a static field. The callers want the live answer:
 * the options enum rebuilds its list of selectable modes whenever the settings screen opens, and
 * the mixin has to be able to notice that support disappeared and fall back to normal VSync.
 */
public final class AdaptiveSyncSupport {

    private AdaptiveSyncSupport() {
    }

    /**
     * Queries GLFW for tear-control support as of this call.
     *
     * @return {@code true} when the driver exposes either the GLX or the WGL tear-control extension
     */
    public static boolean isSupported() {
        return GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear")
                || GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear");
    }
}
