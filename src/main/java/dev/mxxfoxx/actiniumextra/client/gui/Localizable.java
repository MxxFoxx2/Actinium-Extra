package dev.mxxfoxx.actiniumextra.client.gui;

import java.util.function.Function;

/**
 * Contract for an option value that knows how to name itself in the player's language.
 * <p>
 * Every option-backed enum in {@link ActiniumExtraGameOptions} holds a translation key and resolves
 * it through {@code I18n} at the moment the value is drawn, and every cycling option row labels its
 * constants that same way. Declaring the contract once here gives those enums a shared supertype,
 * so the option specification can ask for a label function by interface instead of restating
 * {@code SomeEnum::getLocalizedName} at each call site, and so a new option enum joins by
 * implementing one method.
 * <p>
 * Implementations must resolve the key on every call rather than caching the formatted result: the
 * selected language can change while the game is running, and a cached label would keep rendering
 * the previous language until the row happened to be rebuilt.
 */
@FunctionalInterface
public interface Localizable {

    /**
     * Resolves this value's display name in the currently selected language.
     *
     * @return the localized name, or the untranslated key when no entry exists for it
     */
    String getLocalizedName();

    /**
     * Returns the label function an option specification hands to a cycling row.
     * <p>
     * Every option enum labels its constants identically, so a row that cycles one of them can name
     * the shared contract here instead of repeating {@code SomeEnum::getLocalizedName}.
     *
     * @param <E> the option enum type
     * @return a function mapping any constant of {@code E} to its localized name
     */
    static <E extends Enum<E> & Localizable> Function<E, String> labeler() {
        return Localizable::getLocalizedName;
    }
}
