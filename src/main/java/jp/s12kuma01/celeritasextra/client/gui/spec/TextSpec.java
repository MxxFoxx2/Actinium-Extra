package jp.s12kuma01.celeritasextra.client.gui.spec;

/**
 * A renderer-independent description of a piece of option-UI text.
 * <p>
 * Each supported renderer exposes labels through its own text-component type and offers two
 * flavours: text the addon resolves itself, and a language key the renderer translates on screen.
 * Some labels (the dynamically discovered particle switches) are not keys at all but strings
 * composed at build time, which is why {@link LiteralText} exists next to {@link LiteralKey}.
 * Keeping the choice in the specification lets every renderer adapter reproduce exactly the text
 * the option showed before the adapters existed.
 */
public sealed interface TextSpec {
    /**
     * Finished text the adapters pass through unchanged.
     *
     * @param text the literal text
     * @return the literal-text specification
     */
    static TextSpec literalText(String text) {
        return new LiteralText(text);
    }

    /**
     * Addon language key resolved through {@code I18n} by the spec owner or the adapter, matching
     * the {@code .tooltip} convention used across {@link CeleritasExtraOptionSpecs}.
     *
     * @param key the language key
     * @return the literal-key specification
     */
    static TextSpec literalKey(String key) {
        return new LiteralKey(key);
    }

    /**
     * Language key handed to the renderer so it translates the label itself; used for vanilla keys
     * such as {@code options.vsync}.
     *
     * @param key the language key
     * @return the translatable-key specification
     */
    static TextSpec translatableKey(String key) {
        return new TranslatableKey(key);
    }

    /**
     * Text that is already resolved.
     *
     * @param text the literal text
     */
    record LiteralText(String text) implements TextSpec {
    }

    /**
     * Text taken from a language key and shown as literal text.
     *
     * @param key the language key
     */
    record LiteralKey(String key) implements TextSpec {
    }

    /**
     * Text the renderer translates.
     *
     * @param key the language key
     */
    record TranslatableKey(String key) implements TextSpec {
    }
}
