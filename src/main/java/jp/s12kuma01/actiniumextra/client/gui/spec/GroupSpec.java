package jp.s12kuma01.actiniumextra.client.gui.spec;

import java.util.List;

/**
 * A renderer-independent description of one option group (a visually separated block of rows).
 *
 * @param options the rows, in display order
 */
public record GroupSpec(List<OptionSpec> options) {
    /**
     * @param options the rows, in display order
     * @return the group specification
     */
    public static GroupSpec of(OptionSpec... options) {
        return new GroupSpec(List.of(options));
    }
}
