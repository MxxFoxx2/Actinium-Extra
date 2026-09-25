package dev.mxxfoxx.actiniumextra.renderer;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.Optional;

/**
 * The render pipelines Actinium Extra can attach to.
 * <p>
 * Each entry names the mod id to look for and the adapter class that binds to that pipeline's own
 * copy of the option model. The adapters are referenced by name on purpose: loading one of them
 * pulls in its renderer's classes, so only the adapter of the renderer that is actually installed
 * may ever be touched.
 */
public enum RendererBackend {
    /**
     * Actinium, the Cleanroom renderer that succeeded Celeritas. Checked first: the two pipelines
     * replace the same render paths, so an installation carrying both is already broken, and
     * Actinium is the one whose GUI this addon's pages then land in.
     */
    ACTINIUM("actinium", "Actinium",
            "dev.mxxfoxx.actiniumextra.renderer.actinium.ActiniumRendererGlue"),

    /** Celeritas 2.4.0 or newer, the pipeline this addon was originally written against. */
    CELERITAS("celeritas", "Celeritas",
            "dev.mxxfoxx.actiniumextra.renderer.celeritas.CeleritasRendererGlue");

    private final String modId;
    private final String displayName;
    private final String glueClassName;

    RendererBackend(String modId, String displayName, String glueClassName) {
        this.modId = modId;
        this.displayName = displayName;
        this.glueClassName = glueClassName;
    }

    /**
     * Finds the renderer this game run installed, if any.
     *
     * @return the matching backend, or empty when no supported renderer is loaded
     */
    public static Optional<RendererBackend> detect() {
        for (RendererBackend backend : values()) {
            if (Loader.isModLoaded(backend.modId)) {
                return Optional.of(backend);
            }
        }
        return Optional.empty();
    }

    /**
     * Describes the installed version of this backend for log output.
     *
     * @return the loaded version, or {@code "unknown"} when the mod list does not report one
     */
    public String installedVersion() {
        return Loader.instance().getActiveModList().stream()
                .filter(container -> this.modId.equals(container.getModId()))
                .map(ModContainer::getVersion)
                .findFirst()
                .orElse("unknown");
    }

    /**
     * Creates the adapter that binds Actinium Extra to this backend.
     *
     * @return the live adapter
     * @throws IllegalStateException when the installed backend predates the option API this addon
     *                               binds to, which is reported rather than papered over because
     *                               the extra options would silently never appear
     */
    public RendererGlue createGlue() {
        try {
            Class<?> glueClass = Class.forName(this.glueClassName);
            return (RendererGlue) glueClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError cause) {
            throw new IllegalStateException(
                    "The installed " + this.displayName + " does not expose the option API "
                            + "Actinium Extra was built against; check that both mods are current releases",
                    cause);
        }
    }

    /**
     * @return the name shown in log messages
     */
    public String displayName() {
        return this.displayName;
    }
}
