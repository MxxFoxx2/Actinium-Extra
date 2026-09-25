package dev.mxxfoxx.actiniumextra.client.gui.spec;

import java.util.List;
import java.util.Optional;

/**
 * A renderer-independent description of one option group (a visually separated block of rows).
 *
 * @param options the rows, in display order
 * @param title   an optional header for the group, or {@code null} when the group is anonymous
 */
public record GroupSpec(List<OptionSpec> options, TextSpec title) {
    /**
     * An anonymous group: renderers that support group headers show no header for it.
     *
     * @param options the rows, in display order
     * @return the group specification
     */
    public static GroupSpec of(OptionSpec... options) {
        return new GroupSpec(List.of(options), null);
    }

    /**
     * A titled group. Actinium renders the header above the rows; Celeritas has no group headers,
     * so there the title is simply not shown and the rows still appear in the same order.
     *
     * @param title   the header text
     * @param options the rows, in display order
     * @return the group specification
     */
    public static GroupSpec titled(TextSpec title, OptionSpec... options) {
        return new GroupSpec(List.of(options), title);
    }

    /**
     * @return the group header, if the group has one
     */
    public Optional<TextSpec> titleText() {
        return Optional.ofNullable(title);
    }
}
