package jp.s12kuma01.celeritasextra.client.gui.spec;

import java.util.List;

/**
 * A renderer-independent description of one option page (one tab or sub-tab of the video settings).
 * <p>
 * Actinium requires every contributed page to carry a stable, unique, non-blank
 * {@code namespace:path} identifier, so the identifier is part of the specification instead of
 * being invented by each adapter.
 *
 * @param id     the page identifier
 * @param title  the page label
 * @param groups the groups, in display order
 */
public record PageSpec(PageId id, TextSpec title, List<GroupSpec> groups) {
    /**
     * Identifies a page. Both renderers intern these into their own option-identifier type.
     *
     * @param namespace the owning mod id
     * @param path      the page key
     */
    public record PageId(String namespace, String path) {
    }
}
