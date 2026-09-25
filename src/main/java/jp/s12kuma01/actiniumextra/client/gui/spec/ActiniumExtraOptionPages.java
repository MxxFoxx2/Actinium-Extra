package jp.s12kuma01.actiniumextra.client.gui.spec;

/**
 * Holds the page identifiers of the pages Actinium Extra contributes to the renderer's
 * video-settings UI.
 * <p>
 * Each identifier pairs the {@code actiniumextra} namespace with a page key and serves as a stable
 * handle for the extra pages: both renderers require a non-blank, unique identifier per contributed
 * page and intern these into their own option-identifier type when the pages are built from
 * {@link ActiniumExtraOptionSpecs}. The pages cover animation, particle, detail, render, and
 * miscellaneous ("extra") settings.
 */
public final class ActiniumExtraOptionPages {
    public static final PageSpec.PageId ANIMATION = new PageSpec.PageId("actiniumextra", "animation");
    public static final PageSpec.PageId PARTICLE = new PageSpec.PageId("actiniumextra", "particle");
    public static final PageSpec.PageId DETAILS = new PageSpec.PageId("actiniumextra", "details");
    public static final PageSpec.PageId RENDER = new PageSpec.PageId("actiniumextra", "render");
    public static final PageSpec.PageId EXTRA = new PageSpec.PageId("actiniumextra", "extra");

    private ActiniumExtraOptionPages() {
    }
}
