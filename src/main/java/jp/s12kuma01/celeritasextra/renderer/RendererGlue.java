package jp.s12kuma01.celeritasextra.renderer;

import jp.s12kuma01.celeritasextra.client.gui.WindowModeSupport;
import jp.s12kuma01.celeritasextra.client.gui.spec.CeleritasExtraOptionSpecs;

/**
 * The renderer-side half of Celeritas Extra: one implementation per supported render pipeline.
 * <p>
 * Celeritas and Actinium expose the same option model under different package names, and a
 * renderer accepts only its own classes in its settings screen, so the binding cannot be shared.
 * What is shared is the description of what to contribute
 * ({@link CeleritasExtraOptionSpecs}); an adapter below only converts that description into its
 * renderer's objects and subscribes to its GUI events. One addon jar therefore serves both
 * pipelines, and a single instance of {@link RendererGlue} is live per game run
 * (see {@link RendererSupport}).
 */
public interface RendererGlue {
    /**
     * Whether this renderer already lets the user pick windowed, borderless, or fullscreen mode
     * itself.
     * <p>
     * Actinium ships such an option, so contributing Celeritas Extra's screen-mode replacement next
     * to it would put two controls in charge of the same window state; Celeritas has nothing of the
     * kind, so the replacement fills a real gap there. The value feeds the single
     * {@link CeleritasExtraOptionSpecs#windowRewrites(boolean)} policy call and is the one
     * place a difference in renderer capability reaches the option layout.
     *
     * @return true when the renderer owns a window-mode option
     */
    boolean hasWindowModeOption();

    /**
     * Subscribes this addon's option pages and WINDOW-group replacements to the renderer's GUI
     * construction events.
     * <p>
     * Must be called during mod construction, before the settings screen is first opened: both
     * renderers collect and freeze their page list the first time the GUI is built.
     */
    void attach();

    /**
     * Whether this renderer leaves window management to vanilla, so Celeritas Extra's screen-mode
     * control is the only way to reach borderless mode.
     *
     * @return true when the addon should try to replace the vanilla fullscreen toggle
     */
    default boolean shouldProvideScreenMode() {
        return !hasWindowModeOption() && WindowModeSupport.canOfferBorderless();
    }
}
