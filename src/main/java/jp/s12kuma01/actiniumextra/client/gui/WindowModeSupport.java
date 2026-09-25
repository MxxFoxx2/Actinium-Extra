package jp.s12kuma01.actiniumextra.client.gui;

import jp.s12kuma01.actiniumextra.ActiniumExtraMod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Modifier;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * Gates the desktop screen-mode control without querying the native window.
 * <p>
 * Read by {@link jp.s12kuma01.actiniumextra.renderer.RendererGlue} when it decides whether the
 * addon should replace the renderer's fullscreen toggle.
 */
public final class WindowModeSupport {
    private WindowModeSupport() {
    }

    /**
     * @return true when this desktop can run borderless, so the screen-mode control is worth adding
     */
    public static boolean canOfferBorderless() {
        return SupportHolder.AVAILABLE;
    }

    private static boolean detectSupport() {
        if (isMobileRuntime(System.getenv("POJAV_RENDERER"),
                System.getProperty("os.name", ""), System.getProperty("os.version", ""))) {
            ActiniumExtraMod.LOGGER.info("Keeping the standard fullscreen option on a mobile runtime");
            return false;
        }

        if (!hasRequiredGlfwMethods(GLFW.class)) {
            ActiniumExtraMod.LOGGER.warn(
                    "Keeping the standard fullscreen option: GLFW lacks the required window query methods");
            return false;
        }
        return true;
    }

    static boolean isMobileRuntime(String pojavRenderer, String osName, String osVersion) {
        // Android OpenJDK can report Linux; Zalith/Pojav also identify their renderer this way.
        return (pojavRenderer != null && !pojavRenderer.isBlank())
                || osName.toLowerCase(Locale.ROOT).contains("android")
                || osVersion.toLowerCase(Locale.ROOT).contains("android");
    }

    static boolean hasRequiredGlfwMethods(Class<?> glfwClass) {
        try {
            // Display.isBorderless() uses these IntBuffer overloads. Some mobile shims only
            // provide array overloads. Inspecting signatures neither initializes GLFW nor
            // calls the native window APIs, and is not a guarantee of native support.
            for (String name : new String[]{"glfwGetWindowPos", "glfwGetWindowSize"}) {
                var method = glfwClass.getMethod(name, long.class, IntBuffer.class, IntBuffer.class);
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) {
                    return false;
                }
            }
            return true;
        } catch (NoSuchMethodException | LinkageError | SecurityException e) {
            return false;
        }
    }

    private static final class SupportHolder {
        // Resolve once, when the options GUI first needs the decision.
        private static final boolean AVAILABLE = detectSupport();
    }
}
