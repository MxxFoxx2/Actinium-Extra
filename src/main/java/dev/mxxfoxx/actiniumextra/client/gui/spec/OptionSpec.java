package dev.mxxfoxx.actiniumextra.client.gui.spec;

import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A renderer-independent description of one option row in the video-settings UI.
 * <p>
 * Celeritas and Actinium expose structurally identical option models under different package
 * names, and each renderer only accepts its own classes in its GUI, so the pages Actinium Extra
 * contributes are described here first and turned into renderer objects by the matching adapter in
 * {@code dev.mxxfoxx.actiniumextra.renderer}. Every record binds straight to fields of
 * {@link ActiniumExtraGameOptions} through the same getter/setter pairs the pre-adapter code used.
 * <p>
 * {@link #nameKey()} doubles as the identity of an option: gates refer to parents by it, so it must
 * be unique across the whole specification.
 */
public sealed interface OptionSpec {
    /**
     * The identity of this option, also used as the gate target by dependent options. It is normally
     * the option's language key, except for rows whose label is not a key at all (the dynamically
     * discovered particle switches identify themselves by particle class name).
     *
     * @return the option's identity key
     */
    String nameKey();

    /**
     * The reload side effect the renderer must run when this option's value is applied.
     *
     * @return the flag, or {@link Flag#NONE}
     */
    Flag flag();

    /**
     * The performance-impact hint shown next to the option.
     *
     * @return the impact, or {@link Impact#NONE}
     */
    Impact impact();

    /**
     * When the control is enabled.
     *
     * @return the gate, never {@code null}
     */
    GateSpec gate();

    /**
     * Boolean toggle backed by a {@code TickBoxControl}, named {@code key} with its tooltip read
     * from {@code key.tooltip}.
     */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, Flag.NONE, Impact.NONE, GateSpec.always());
    }

    /** Boolean toggle with a performance-impact hint. */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     Impact impact) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, Flag.NONE, impact, GateSpec.always());
    }

    /** Boolean toggle with a reload flag. */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     Flag flag) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, flag, Impact.NONE, GateSpec.always());
    }

    /** Boolean toggle with a reload flag and a performance-impact hint. */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     Flag flag,
                     Impact impact) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, flag, impact, GateSpec.always());
    }

    /** Boolean toggle that greys out with its parent option. */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     GateSpec gate) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, Flag.NONE, Impact.NONE, gate);
    }

    /** Boolean toggle with a reload flag that greys out with its parent option. */
    static Bool bool(String key,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     Flag flag,
                     GateSpec gate) {
        return new Bool(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                setter, getter, flag, Impact.NONE, gate);
    }

    /** Boolean toggle with fully explicit texts, used by the dynamically discovered options. */
    static Bool bool(String key,
                     TextSpec name,
                     TextSpec tooltip,
                     BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                     Function<ActiniumExtraGameOptions, Boolean> getter,
                     GateSpec gate) {
        return new Bool(key, name, tooltip, setter, getter, Flag.NONE, Impact.NONE, gate);
    }

    /** Integer slider that greys out with its parent option. */
    static Slider slider(String key,
                         int min,
                         int max,
                         int step,
                         FormatterSpec formatter,
                         BiConsumer<ActiniumExtraGameOptions, Integer> setter,
                         Function<ActiniumExtraGameOptions, Integer> getter,
                         GateSpec gate) {
        return new Slider(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                min, max, step, formatter, setter, getter, Flag.NONE, Impact.NONE, gate);
    }

    /** Integer slider with a performance-impact hint that greys out with its parent option. */
    static Slider slider(String key,
                         int min,
                         int max,
                         int step,
                         FormatterSpec formatter,
                         BiConsumer<ActiniumExtraGameOptions, Integer> setter,
                         Function<ActiniumExtraGameOptions, Integer> getter,
                         GateSpec gate,
                         Impact impact) {
        return new Slider(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                min, max, step, formatter, setter, getter, Flag.NONE, impact, gate);
    }

    /** Integer slider with a performance-impact hint and a reload flag. */
    static Slider slider(String key,
                         int min,
                         int max,
                         int step,
                         FormatterSpec formatter,
                         BiConsumer<ActiniumExtraGameOptions, Integer> setter,
                         Function<ActiniumExtraGameOptions, Integer> getter,
                         GateSpec gate,
                         Impact impact,
                         Flag flag) {
        return new Slider(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                min, max, step, formatter, setter, getter, flag, impact, gate);
    }

    /**
     * Enum cycle backed by a {@code CyclingControl} named {@code key} like {@link #bool}; the
     * adapter derives the control's labels by applying {@code label} to each constant of
     * {@code type} in declaration order.
     */
    static <T extends Enum<T>> Cycling<T> cycling(String key,
                                                 Class<T> type,
                                                 Function<T, String> label,
                                                 BiConsumer<ActiniumExtraGameOptions, T> setter,
                                                 Function<ActiniumExtraGameOptions, T> getter) {
        return cycling(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                type, label, setter, getter, Impact.NONE);
    }

    /** Enum cycle that greys out with its parent option. */
    static <T extends Enum<T>> Cycling<T> cycling(String key,
                                                 Class<T> type,
                                                 Function<T, String> label,
                                                 BiConsumer<ActiniumExtraGameOptions, T> setter,
                                                 Function<ActiniumExtraGameOptions, T> getter,
                                                 GateSpec gate) {
        return new Cycling<>(key, TextSpec.literalKey(key), TextSpec.literalKey(key + ".tooltip"),
                type, label, setter, getter, Flag.NONE, Impact.NONE, gate);
    }

    /** Enum cycle with fully explicit texts, used where the label is a vanilla language key. */
    static <T extends Enum<T>> Cycling<T> cycling(String key,
                                                 TextSpec name,
                                                 TextSpec tooltip,
                                                 Class<T> type,
                                                 Function<T, String> label,
                                                 BiConsumer<ActiniumExtraGameOptions, T> setter,
                                                 Function<ActiniumExtraGameOptions, T> getter,
                                                 Impact impact) {
        return new Cycling<>(key, name, tooltip, type, label, setter, getter,
                Flag.NONE, impact, GateSpec.always());
    }

    /**
     * Reload side effects, mirroring the flags both renderers expose.
     */
    enum Flag {
        /** No reload is requested. */
        NONE,
        /** The renderer must reload assets. */
        REQUIRES_ASSET_RELOAD,
        /** The renderer must reload the chunk renderer. */
        REQUIRES_RENDERER_RELOAD
    }

    /**
     * Performance-impact hints, mirroring the levels both renderers expose.
     */
    enum Impact {
        /** No hint is shown. */
        NONE,
        /** The change is nearly free. */
        LOW,
        /** The change is measurable. */
        MEDIUM,
        /** The change is significant. */
        HIGH,
        /** The cost depends on the scene. */
        VARIES
    }

    /**
     * A toggle row.
     *
     * @param nameKey  identity and default language key
     * @param name     row label
     * @param tooltip  row tooltip
     * @param setter   writes the value into the addon config
     * @param getter   reads the value from the addon config
     * @param flag     reload side effect
     * @param impact   performance hint
     * @param gate     enable condition
     */
    record Bool(String nameKey,
                TextSpec name,
                TextSpec tooltip,
                BiConsumer<ActiniumExtraGameOptions, Boolean> setter,
                Function<ActiniumExtraGameOptions, Boolean> getter,
                Flag flag,
                Impact impact,
                GateSpec gate) implements OptionSpec {
    }

    /**
     * An integer slider row over the inclusive {@code [min, max]} range.
     *
     * @param nameKey   identity and default language key
     * @param name      row label
     * @param tooltip   row tooltip
     * @param min       inclusive minimum
     * @param max       inclusive maximum
     * @param step      increment between values
     * @param formatter how the current value is displayed
     * @param setter    writes the value into the addon config
     * @param getter    reads the value from the addon config
     * @param flag      reload side effect
     * @param impact    performance hint
     * @param gate      enable condition
     */
    record Slider(String nameKey,
                  TextSpec name,
                  TextSpec tooltip,
                  int min,
                  int max,
                  int step,
                  FormatterSpec formatter,
                  BiConsumer<ActiniumExtraGameOptions, Integer> setter,
                  Function<ActiniumExtraGameOptions, Integer> getter,
                  Flag flag,
                  Impact impact,
                  GateSpec gate) implements OptionSpec {
    }

    /**
     * An enum cycling row.
     *
     * @param nameKey identity; derived from the enum type because these rows have no per-option key
     * @param name    row label
     * @param tooltip row tooltip
     * @param type    the enum class
     * @param label   how each constant is labelled
     * @param setter  writes the value into the addon config
     * @param getter  reads the value from the addon config
     * @param flag    reload side effect
     * @param impact  performance hint
     * @param gate    enable condition
     * @param <T>     the enum type
     */
    record Cycling<T extends Enum<T>>(String nameKey,
                                      TextSpec name,
                                      TextSpec tooltip,
                                      Class<T> type,
                                      Function<T, String> label,
                                      BiConsumer<ActiniumExtraGameOptions, T> setter,
                                      Function<ActiniumExtraGameOptions, T> getter,
                                      Flag flag,
                                      Impact impact,
                                      GateSpec gate) implements OptionSpec {
    }
}
