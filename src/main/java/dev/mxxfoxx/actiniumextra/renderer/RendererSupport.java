package dev.mxxfoxx.actiniumextra.renderer;

import dev.mxxfoxx.actiniumextra.ActiniumExtraMod;

import java.util.Optional;

/**
 * Chooses the live renderer adapter once per game run and wires Actinium Extra into it.
 * <p>
 * The Forge dependency list cannot express "either of these two mods", so the renderer is detected
 * here instead of being declared: {@link #initialize()} runs during mod construction, which is also
 * the point both renderers require, since they freeze their option-page list the first time the
 * settings screen is opened.
 */
public final class RendererSupport {
    private RendererSupport() {
    }

    /**
     * Detects the installed renderer and subscribes this addon's option contributions to it.
     * <p>
     * The adapter itself needs no retention: the renderer's event bus holds the registered
     * listeners for the lifetime of the run.
     *
     * @throws IllegalStateException when a renderer is installed but does not expose the option API
     *                               this addon binds to
     */
    public static void initialize() {
        Optional<RendererBackend> detected = RendererBackend.detect();

        if (detected.isEmpty()) {
            ActiniumExtraMod.LOGGER.error(
                    "No supported renderer found: Actinium Extra contributes its settings to the video-settings"
                            + " screen of Celeritas (2.4.0 or newer) or Actinium, so without one of them the extra"
                            + " options cannot be reached. Install a supported renderer and restart.");
            return;
        }

        RendererBackend backend = detected.get();
        RendererGlue glue = backend.createGlue();
        glue.attach();

        ActiniumExtraMod.LOGGER.info("Registered Actinium Extra options with {} {}",
                backend.displayName(), backend.installedVersion());
    }
}
