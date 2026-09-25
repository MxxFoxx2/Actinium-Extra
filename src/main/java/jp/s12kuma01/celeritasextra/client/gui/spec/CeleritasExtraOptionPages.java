package jp.s12kuma01.celeritasextra.client.gui.spec;

/**
 * Holds the page identifiers of the pages Celeritas Extra contributes to the renderer's
 * video-settings UI.
 * <p>
 * Each identifier pairs the {@code celeritasextra} namespace with a page key and serves as a stable
 * handle for the extra pages: both renderers require a non-blank, unique identifier per contributed
 * page and intern these into their own option-identifier type when the pages are built from
 * {@link CeleritasExtraOptionSpecs}. The pages cover animation, particle, detail, render, and
 * miscellaneous ("extra") settings.
 */
public final class CeleritasExtraOptionPages {
    public static final PageSpec.PageId ANIMATION = new PageSpec.PageId("celeritasextra", "animation");
    public static final PageSpec.PageId PARTICLE = new PageSpec.PageId("celeritasextra", "particle");
    public static final PageSpec.PageId DETAILS = new PageSpec.PageId("celeritasextra", "details");
    public static final PageSpec.PageId RENDER = new PageSpec.PageId("celeritasextra", "render");
    public static final PageSpec.PageId EXTRA = new PageSpec.PageId("celeritasextra", "extra");

    private CeleritasExtraOptionPages() {
    }
}
