package jp.s12kuma01.celeritasextra.renderer.celeritas;

import jp.s12kuma01.celeritasextra.CeleritasExtraMod;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions;
import jp.s12kuma01.celeritasextra.client.gui.spec.CeleritasExtraOptionSpecs;
import jp.s12kuma01.celeritasextra.client.gui.spec.FormatterSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.GateSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.GroupSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.OptionSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.PageSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.StandardRewriteSpec;
import jp.s12kuma01.celeritasextra.client.gui.spec.TextSpec;
import jp.s12kuma01.celeritasextra.renderer.RendererGlue;
import net.minecraft.client.resources.I18n;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.StandardOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Binds Celeritas Extra to Celeritas' option model ({@code org.taumc.celeritas.api}).
 * <p>
 * Celeritas only accepts its own option classes in its settings screen, so every row Celeritas
 * Extra contributes is converted from the shared specification here. This class is the only place
 * in the addon, besides {@link CeleritasOptionStorage}, that may reference a Celeritas type: it is
 * instantiated reflectively by {@link jp.s12kuma01.celeritasextra.renderer.RendererBackend} once
 * Celeritas has been detected, so a game run without Celeritas never loads it.
 */
public final class CeleritasRendererGlue implements RendererGlue {
    private final CeleritasOptionStorage storage = new CeleritasOptionStorage();

    @Override
    public boolean hasWindowModeOption() {
        return false;
    }

    @Override
    public void attach() {
        OptionGUIConstructionEvent.BUS.addListener(this::onGuiConstruct);
        OptionGroupConstructionEvent.BUS.addListener(this::onGroupConstruct);
    }

    /**
     * Adds the five contributed pages when Celeritas builds its settings screen.
     *
     * @param event the GUI construction event
     */
    private void onGuiConstruct(OptionGUIConstructionEvent event) {
        List<PageSpec> pages = CeleritasExtraOptionSpecs.pages();
        for (PageSpec page : pages) {
            event.addPage(toPage(page));
        }
        CeleritasExtraMod.LOGGER.info("Registered {} Celeritas Extra option pages", pages.size());
    }

    /**
     * Replaces Celeritas' own fullscreen and vsync rows in the WINDOW group.
     *
     * @param event the group construction event
     */
    private void onGroupConstruct(OptionGroupConstructionEvent event) {
        if (!event.getId().matches(StandardOptions.Group.WINDOW)) {
            return;
        }

        List<StandardRewriteSpec> rewrites =
                CeleritasExtraOptionSpecs.windowRewrites(shouldProvideScreenMode());
        List<Option<?>> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            Option<?> option = options.get(i);
            StandardRewriteSpec.StandardId target = standardIdOf(option.getId());
            if (target == null) {
                continue;
            }
            for (StandardRewriteSpec rewrite : rewrites) {
                if (rewrite.standard() != target) {
                    continue;
                }
                options.set(i, toOption(rewrite.option(), new HashMap<>(), target));
                CeleritasExtraMod.LOGGER.debug("Replaced the Celeritas {} option with the Celeritas Extra control",
                        target);
            }
        }
    }

    /**
     * Maps a Celeritas option identifier onto the row Celeritas Extra knows how to replace.
     *
     * @param id the identifier of a row in the WINDOW group, possibly {@code null}
     * @return the targeted row, or {@code null} when Celeritas Extra has no replacement for it
     */
    private static StandardRewriteSpec.StandardId standardIdOf(OptionIdentifier<?> id) {
        if (id == null) {
            return null;
        }
        if (id.matches(StandardOptions.Option.FULLSCREEN)) {
            return StandardRewriteSpec.StandardId.FULLSCREEN;
        }
        if (id.matches(StandardOptions.Option.VSYNC)) {
            return StandardRewriteSpec.StandardId.VSYNC;
        }
        return null;
    }

    /**
     * Converts a page specification into Celeritas' page model.
     *
     * @param page the page to convert
     * @return the Celeritas page
     */
    private OptionPage toPage(PageSpec page) {
        Map<String, Option<?>> built = new HashMap<>();
        List<OptionGroup> groups = new ArrayList<>();
        for (GroupSpec group : page.groups()) {
            OptionGroup.Builder builder = OptionGroup.createBuilder();
            for (OptionSpec option : group.options()) {
                builder.add(toOption(option, built, null));
            }
            groups.add(builder.build());
        }
        return new OptionPage(OptionIdentifier.create(page.id().namespace(), page.id().path()),
                text(page.title()), List.copyOf(groups));
    }

    /**
     * Converts one option row, recording it so dependent rows can gate on its live value.
     *
     * @param spec     the row to convert
     * @param built    the rows converted so far in this page, keyed by their identity key
     * @param standard the WINDOW-group row this replacement takes over, or {@code null}
     * @return the Celeritas option
     */
    private Option<?> toOption(OptionSpec spec, Map<String, Option<?>> built,
                               StandardRewriteSpec.StandardId standard) {
        Option<?> option = switch (spec) {
            case OptionSpec.Bool bool -> toBool(bool, built, standard);
            case OptionSpec.Slider slider -> toSlider(slider, built, standard);
            case OptionSpec.Cycling<?> cycling -> toCycling(cycling, built, standard);
        };
        built.put(spec.nameKey(), option);
        return option;
    }

    private Option<?> toBool(OptionSpec.Bool spec, Map<String, Option<?>> built,
                             StandardRewriteSpec.StandardId standard) {
        OptionImpl.Builder<CeleritasExtraGameOptions, Boolean> builder = OptionImpl
                .createBuilder(boolean.class, storage)
                .setName(text(spec.name()))
                .setTooltip(text(spec.tooltip()))
                .setControl(TickBoxControl::new)
                .setBinding(spec.setter(), spec.getter());
        applyShared(builder, spec, built, standard);
        return builder.build();
    }

    private Option<?> toSlider(OptionSpec.Slider spec, Map<String, Option<?>> built,
                              StandardRewriteSpec.StandardId standard) {
        OptionImpl.Builder<CeleritasExtraGameOptions, Integer> builder = OptionImpl
                .createBuilder(int.class, storage)
                .setName(text(spec.name()))
                .setTooltip(text(spec.tooltip()))
                .setControl(option -> new SliderControl(option, spec.min(), spec.max(), spec.step(),
                        formatter(spec.formatter())))
                .setBinding(spec.setter(), spec.getter());
        applyShared(builder, spec, built, standard);
        return builder.build();
    }

    private <T extends Enum<T>> Option<?> toCycling(OptionSpec.Cycling<T> spec,
                                                    Map<String, Option<?>> built,
                                                    StandardRewriteSpec.StandardId standard) {
        OptionImpl.Builder<CeleritasExtraGameOptions, T> builder = OptionImpl
                .createBuilder(spec.type(), storage)
                .setName(text(spec.name()))
                .setTooltip(text(spec.tooltip()))
                .setControl(option -> new CyclingControl<>(option, spec.type(),
                        names(spec.type(), spec.label())))
                .setBinding(spec.setter(), spec.getter());
        applyShared(builder, spec, built, standard);
        return builder.build();
    }

    /**
     * Applies the identity, reload flag, impact hint and enable gate shared by every row kind.
     *
     * @param builder  the row under construction
     * @param spec     the row specification
     * @param built    the rows converted so far in this page
     * @param standard the WINDOW-group row this replacement takes over, or {@code null}
     * @param <T>      the row's value type
     */
    private <T> void applyShared(OptionImpl.Builder<CeleritasExtraGameOptions, T> builder,
                                 OptionSpec spec, Map<String, Option<?>> built,
                                 StandardRewriteSpec.StandardId standard) {
        if (standard != null) {
            builder.setId(standardIdentifier(standard).cast());
        }
        if (spec.flag() != OptionSpec.Flag.NONE) {
            builder.setFlags(flag(spec.flag()));
        }
        if (spec.impact() != OptionSpec.Impact.NONE) {
            builder.setImpact(impact(spec.impact()));
        }
        builder.setEnabledPredicate(gate(spec.gate(), built));
    }

    /**
     * @param standard the WINDOW-group row being replaced
     * @return Celeritas' identifier for that row
     */
    private static OptionIdentifier<Void> standardIdentifier(StandardRewriteSpec.StandardId standard) {
        return switch (standard) {
            case FULLSCREEN -> StandardOptions.Option.FULLSCREEN;
            case VSYNC -> StandardOptions.Option.VSYNC;
        };
    }

    /**
     * Resolves a gate into a live predicate over the rows converted so far.
     *
     * @param gate  the gate to resolve
     * @param built the rows converted so far in this page
     * @return the predicate Celeritas evaluates while painting the row
     */
    private static BooleanSupplier gate(GateSpec gate, Map<String, Option<?>> built) {
        return switch (gate) {
            case GateSpec.Always ignored -> () -> true;
            case GateSpec.ByKey key -> liveValue(built, key.optionKey());
            case GateSpec.Live live -> live.supplier();
            case GateSpec.All all -> {
                List<BooleanSupplier> parts = all.gates().stream().map(part -> gate(part, built)).toList();
                yield () -> parts.stream().allMatch(BooleanSupplier::getAsBoolean);
            }
        };
    }

    /**
     * Reads the parent row's pending value, so sub-rows grey out while the user edits the parent
     * and before anything is applied.
     *
     * @param built the rows converted so far in this page
     * @param key   the parent row's identity key
     * @return the parent's live value
     */
    @SuppressWarnings("unchecked")
    private static BooleanSupplier liveValue(Map<String, Option<?>> built, String key) {
        Option<Boolean> parent = (Option<Boolean>) built.get(key);
        if (parent == null) {
            throw new IllegalStateException("An option gates on '" + key
                    + "', which the specification does not build before it");
        }
        return parent::getValue;
    }

    /**
     * @param spec the text specification
     * @return the Celeritas text component
     */
    private static TextComponent text(TextSpec spec) {
        return switch (spec) {
            case TextSpec.LiteralText literal -> TextComponent.literal(literal.text());
            case TextSpec.LiteralKey key -> TextComponent.literal(I18n.format(key.key()));
            case TextSpec.TranslatableKey key -> TextComponent.translatable(key.key());
        };
    }

    /**
     * @param type  the enum class
     * @param label the per-constant label
     * @param <T>   the enum type
     * @return the cycle labels, in enum declaration order
     */
    private static <T extends Enum<T>> TextComponent[] names(Class<T> type, Function<T, String> label) {
        T[] constants = type.getEnumConstants();
        TextComponent[] names = new TextComponent[constants.length];
        for (int i = 0; i < constants.length; i++) {
            names[i] = TextComponent.literal(label.apply(constants[i]));
        }
        return names;
    }

    /**
     * @param spec the formatter specification
     * @return the matching Celeritas value formatter
     */
    private static ControlValueFormatter formatter(FormatterSpec spec) {
        return switch (spec) {
            case FormatterSpec.Number ignored -> ControlValueFormatter.number();
            case FormatterSpec.Percentage ignored -> ControlValueFormatter.percentage();
            case FormatterSpec.QuantityOrDisabled quantity ->
                    ControlValueFormatter.quantityOrDisabled(quantity.unit(), quantity.disabledText());
            case FormatterSpec.Custom custom -> value -> TextComponent.literal(custom.label().apply(value));
        };
    }

    /**
     * @param flag the spec flag
     * @return the matching Celeritas reload flag
     */
    private static OptionFlag flag(OptionSpec.Flag flag) {
        return switch (flag) {
            case REQUIRES_ASSET_RELOAD -> OptionFlag.REQUIRES_ASSET_RELOAD;
            case REQUIRES_RENDERER_RELOAD -> OptionFlag.REQUIRES_RENDERER_RELOAD;
            case NONE -> throw new IllegalStateException("Flag.NONE must not be converted");
        };
    }

    /**
     * @param impact the spec impact
     * @return the matching Celeritas impact level
     */
    private static OptionImpact impact(OptionSpec.Impact impact) {
        return switch (impact) {
            case LOW -> OptionImpact.LOW;
            case MEDIUM -> OptionImpact.MEDIUM;
            case HIGH -> OptionImpact.HIGH;
            case VARIES -> OptionImpact.VARIES;
            case NONE -> throw new IllegalStateException("Impact.NONE must not be converted");
        };
    }
}
