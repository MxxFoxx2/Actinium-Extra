package jp.s12kuma01.celeritasextra.client.gui.spec;

/**
 * A request to replace one of the renderer's own rows in its WINDOW group.
 * <p>
 * Celeritas Extra swaps the plain Fullscreen and VSync toggles for richer three-way controls.
 * Whether a swap makes sense depends on the renderer: Actinium already ships a windowed /
 * borderless / exclusive-fullscreen option of its own, so replacing its vanilla fullscreen toggle
 * would give the user two controls fighting over the same state. That decision is made once in
 * {@link CeleritasExtraOptionSpecs#windowRewrites(boolean)} and expressed here as the set
 * of standard rows to override, which each adapter then looks up in its own identifier constants.
 *
 * @param standard the renderer row to replace
 * @param option   the replacement row
 */
public record StandardRewriteSpec(StandardId standard, OptionSpec option) {
    /**
     * Rows of the renderer's WINDOW group that Celeritas Extra knows how to replace.
     */
    public enum StandardId {
        /** The vanilla fullscreen toggle. */
        FULLSCREEN,
        /** The vanilla vertical-sync toggle. */
        VSYNC
    }
}
