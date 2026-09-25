package jp.s12kuma01.actiniumextra.client.gui.spec;

import java.util.function.Function;

/**
 * A renderer-independent description of how an integer option renders its current value.
 * <p>
 * The fixed kinds mirror the value formatters both renderers expose under the same names, so the
 * adapters map them one to one and the on-screen text stays byte-identical to what each renderer
 * produced before this indirection existed. {@link Custom} covers the addon-specific spellings
 * (cloud scale, tick intervals) that have no shared formatter.
 */
public sealed interface FormatterSpec {
    /**
     * Plain number formatting, as offered by both renderers.
     *
     * @return the number formatter specification
     */
    static FormatterSpec number() {
        return new Number();
    }

    /**
     * Percentage formatting, as offered by both renderers.
     *
     * @return the percentage formatter specification
     */
    static FormatterSpec percentage() {
        return new Percentage();
    }

    /**
     * Quantity formatting with a sentinel value that reads as "off" or "default".
     *
     * @param unit         the unit language key, for example {@code chunks}
     * @param disabledText the literal text used for the minimum value
     * @return the quantity-or-disabled formatter specification
     */
    static FormatterSpec quantityOrDisabled(String unit, String disabledText) {
        return new QuantityOrDisabled(unit, disabledText);
    }

    /**
     * Formatting the addon owns outright.
     *
     * @param label the value-to-text function; the adapter wraps its result as literal text
     * @return the custom formatter specification
     */
    static FormatterSpec custom(Function<Integer, String> label) {
        return new Custom(label);
    }

    /** Maps to the renderers' {@code ControlValueFormatter.number()}. */
    record Number() implements FormatterSpec {
    }

    /** Maps to the renderers' {@code ControlValueFormatter.percentage()}. */
    record Percentage() implements FormatterSpec {
    }

    /**
     * Maps to the renderers' {@code ControlValueFormatter.quantityOrDisabled(unit, disabledText)}.
     *
     * @param unit         the unit language key
     * @param disabledText the literal text used for the minimum value
     */
    record QuantityOrDisabled(String unit, String disabledText) implements FormatterSpec {
    }

    /**
     * Addon-side formatting.
     *
     * @param label the value-to-text function
     */
    record Custom(Function<Integer, String> label) implements FormatterSpec {
    }
}
