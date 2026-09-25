package dev.mxxfoxx.actiniumextra.client.gui.spec;

import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the contributed option layout, so the renderer adapters cannot drift the pages away from
 * what Actinium Extra has always shown.
 * <p>
 * The adapters build their rows strictly in specification order and resolve a
 * {@link GateSpec.ByKey} against the rows they converted before, so a page whose keys or order
 * change without a matching expectation here breaks both renderers, not just one of them.
 */
class ActiniumExtraOptionSpecsTest {
    private static final String ANIMATIONS = "actiniumextra.option.animations.";
    private static final String PARTICLES = "actiniumextra.option.particles.";
    private static final String DETAILS = "actiniumextra.option.details.";
    private static final String RENDER = "actiniumextra.option.render.";
    private static final String EXTRA = "actiniumextra.option.extra.";

    @Test
    void contributesTheFivePagesInTheirTabOrder() {
        List<PageSpec> pages = ActiniumExtraOptionSpecs.pages();

        assertEquals(5, pages.size());
        assertEquals(List.of("animation", "particle", "details", "render", "extra"),
                pages.stream().map(page -> page.id().path()).toList());
        assertTrue(pages.stream().allMatch(page -> "actiniumextra".equals(page.id().namespace())),
                "Every contributed page must stay in the addon's own namespace");
    }

    @Test
    void animationsPageGatesEveryTypeOnTheMasterSwitch() {
        PageSpec page = ActiniumExtraOptionSpecs.animations();

        assertEquals(List.of(ANIMATIONS + "all"), keys(page.groups().get(0)));
        assertEquals(List.of(ANIMATIONS + "water", ANIMATIONS + "lava", ANIMATIONS + "fire",
                ANIMATIONS + "portal", ANIMATIONS + "block"), keys(page.groups().get(1)));

        OptionSpec.Bool master = assertInstanceOf(OptionSpec.Bool.class, page.groups().get(0).options().get(0));
        assertEquals(OptionSpec.Flag.REQUIRES_ASSET_RELOAD, master.flag());
        assertEquals(OptionSpec.Impact.MEDIUM, master.impact());
        assertEquals(GateSpec.always(), master.gate());

        OptionSpec.Bool water = assertInstanceOf(OptionSpec.Bool.class, page.groups().get(1).options().get(0));
        assertEquals(new GateSpec.ByKey(ANIMATIONS + "all"), water.gate());
        assertEquals(OptionSpec.Flag.REQUIRES_ASSET_RELOAD, water.flag());
    }

    @Test
    void particlesPageKeepsItsStaticTogglesRegardlessOfDiscovery() {
        PageSpec page = ActiniumExtraOptionSpecs.particles();

        assertEquals(List.of(PARTICLES + "all"), keys(page.groups().get(0)));
        assertEquals(List.of(PARTICLES + "rain_splash", PARTICLES + "block_break",
                PARTICLES + "block_breaking"), keys(page.groups().get(1)));
        assertEquals(TextSpec.literalKey("actiniumextra.option.group.builtin_particles"),
                page.groups().get(1).title());
        assertTrue(page.groups().get(0).titleText().isEmpty(),
                "the master particles switch stays in an anonymous group");

        OptionSpec.Bool master = assertInstanceOf(OptionSpec.Bool.class, page.groups().get(0).options().get(0));
        assertEquals(OptionSpec.Impact.HIGH, master.impact());
        assertEquals(OptionSpec.Flag.NONE, master.flag());
    }

    @Test
    void detailsPageKeepsItsToggleOrderAndStarSlider() {
        PageSpec page = ActiniumExtraOptionSpecs.details();

        assertEquals(List.of(DETAILS + "sky", DETAILS + "stars", DETAILS + "total_stars",
                DETAILS + "sun_moon", DETAILS + "rain_snow"), keys(page.groups().get(0)));
        assertEquals(List.of(DETAILS + "biome_colors", DETAILS + "sky_colors",
                DETAILS + "void_fog"), keys(page.groups().get(1)));
        assertEquals(List.of(TextSpec.literalKey("actiniumextra.option.group.sky"),
                TextSpec.literalKey("actiniumextra.option.group.colors")),
                page.groups().stream().map(GroupSpec::title).toList());

        OptionSpec.Slider stars = assertInstanceOf(OptionSpec.Slider.class, page.groups().get(0).options().get(2));
        assertEquals(500, stars.min());
        assertEquals(32000, stars.max());
        assertEquals(500, stars.step());
        assertEquals(new GateSpec.ByKey(DETAILS + "stars"), stars.gate());
        assertEquals(OptionSpec.Impact.MEDIUM, stars.impact());
        assertEquals(OptionSpec.Flag.REQUIRES_RENDERER_RELOAD, stars.flag());
    }

    @Test
    void renderPageGroupsFogCloudLightingAndEntitiesWithoutReorderingRows() {
        PageSpec page = ActiniumExtraOptionSpecs.render();

        assertEquals(List.of(RENDER + "fog", RENDER + "fog_start", RENDER + "fog_distance",
                RENDER + "prevent_shaders", RENDER + "clouds", RENDER + "modern_clouds",
                RENDER + "cloud_height", RENDER + "cloud_distance", RENDER + "cloud_scale",
                RENDER + "cloud_translucency", RENDER + "light_updates", RENDER + "item_frames",
                RENDER + "item_frame_lod_distance", RENDER + "armor_stands", RENDER + "paintings",
                RENDER + "beacons", RENDER + "limit_beacon_beam_height", RENDER + "pistons",
                RENDER + "enchanting_books", RENDER + "player_name_tag", RENDER + "item_frame_name_tag"),
                page.groups().stream().flatMap(group -> group.options().stream())
                        .map(OptionSpec::nameKey).toList());
        assertEquals(List.of(TextSpec.literalKey("actiniumextra.option.group.fog"),
                TextSpec.literalKey("actiniumextra.option.group.clouds"),
                TextSpec.literalKey("actiniumextra.option.group.lighting"),
                TextSpec.literalKey("actiniumextra.option.group.entities")),
                page.groups().stream().map(GroupSpec::title).toList());

        List<OptionSpec> rows = page.groups().stream()
                .flatMap(group -> group.options().stream()).toList();
        OptionSpec.Bool modernClouds = assertInstanceOf(OptionSpec.Bool.class, rows.get(5));
        assertEquals(OptionSpec.Flag.REQUIRES_ASSET_RELOAD, modernClouds.flag());
        GateSpec.All gate = assertInstanceOf(GateSpec.All.class, modernClouds.gate());
        assertEquals(List.of(new GateSpec.ByKey(RENDER + "clouds")),
                gate.gates().stream().filter(GateSpec.ByKey.class::isInstance).toList());
        assertEquals(1, gate.gates().stream().filter(GateSpec.Live.class::isInstance).count());

        OptionSpec.Slider cloudHeight = assertInstanceOf(OptionSpec.Slider.class, rows.get(6));
        assertEquals(ActiniumExtraGameOptions.RenderSettings.USE_WORLD_CLOUD_HEIGHT, cloudHeight.min());
        assertEquals(384, cloudHeight.max());

        OptionSpec.Slider cloudScale = assertInstanceOf(OptionSpec.Slider.class, rows.get(8));
        assertEquals(ActiniumExtraGameOptions.RenderSettings.CLOUD_SCALE_MIN, cloudScale.min());
        assertEquals(ActiniumExtraGameOptions.RenderSettings.CLOUD_SCALE_MAX, cloudScale.max());
    }

    @Test
    void extraPageKeepsItsOverlayMiscellaneousAndToastGroups() {
        PageSpec page = ActiniumExtraOptionSpecs.extra();

        assertEquals(List.of(EXTRA + "fps", EXTRA + "fps_extended", EXTRA + "coords",
                EXTRA + "ignore_reduced_debug_info", EXTRA + "overlay_corner", EXTRA + "text_contrast"),
                keys(page.groups().get(0)));
        assertEquals(List.of(EXTRA + "mod_name_tooltip", EXTRA + "steady_debug_hud",
                EXTRA + "steady_debug_hud_refresh"), keys(page.groups().get(1)));
        assertEquals(List.of(EXTRA + "toasts", EXTRA + "toast_advancement", EXTRA + "toast_recipe",
                EXTRA + "toast_tutorial", EXTRA + "toast_system"), keys(page.groups().get(2)));
        assertEquals(List.of(TextSpec.literalKey("actiniumextra.option.group.overlays"),
                TextSpec.literalKey("actiniumextra.option.group.interface"),
                TextSpec.literalKey("actiniumextra.option.group.toasts")),
                page.groups().stream().map(GroupSpec::title).toList());

        OptionSpec cornerSpec = page.groups().get(0).options().get(4);
        assertEquals(OptionSpec.Cycling.class, cornerSpec.getClass());
        OptionSpec.Cycling<?> corner = (OptionSpec.Cycling<?>) cornerSpec;
        assertEquals(ActiniumExtraGameOptions.OverlayCorner.class, corner.type());
    }

    @Test
    void everyGateTargetsAnEarlierBooleanOption() {
        for (PageSpec page : ActiniumExtraOptionSpecs.pages()) {
            Map<String, OptionSpec> built = new LinkedHashMap<>();
            for (GroupSpec group : page.groups()) {
                for (OptionSpec option : group.options()) {
                    assertGateResolvable(page, option.nameKey(), option.gate(), built);
                    built.put(option.nameKey(), option);
                }
            }
        }
    }

    @Test
    void optionKeysAreUniqueAcrossTheWholeContribution() {
        List<String> keys = new ArrayList<>();
        for (PageSpec page : ActiniumExtraOptionSpecs.pages()) {
            for (GroupSpec group : page.groups()) {
                keys.addAll(keys(group));
            }
        }

        assertEquals(keys.size(), new HashSet<>(keys).size(),
                "Duplicate option key: " + duplicates(keys));
    }

    @Test
    void actiniumOnlyGainsAdaptiveVSyncBecauseItOwnsWindowModes() {
        // Actinium reports hasWindowModeOption(), so its adapter never asks for the screen-mode row.
        assertEquals(List.of(StandardRewriteSpec.StandardId.VSYNC), rewriteTargets(false));
    }

    @Test
    void celeritasGainsTheScreenModeReplacementWhenTheDesktopSupportsIt() {
        // Celeritas leaves window management to vanilla on a desktop that can run borderless.
        assertEquals(List.of(StandardRewriteSpec.StandardId.FULLSCREEN, StandardRewriteSpec.StandardId.VSYNC),
                rewriteTargets(true));
    }

    @Test
    void windowReplacementsKeepTheirTargetRowTexts() {
        List<StandardRewriteSpec> rewrites = ActiniumExtraOptionSpecs.windowRewrites(true);

        OptionSpec screenModeSpec = rewrites.get(0).option();
        assertEquals(OptionSpec.Cycling.class, screenModeSpec.getClass());
        OptionSpec.Cycling<?> screenMode = (OptionSpec.Cycling<?>) screenModeSpec;
        assertEquals("actiniumextra.option.screen_mode", screenMode.nameKey());
        assertEquals(new TextSpec.LiteralKey("actiniumextra.option.screen_mode"), screenMode.name());
        assertEquals(ActiniumExtraGameOptions.ScreenMode.class, screenMode.type());

        OptionSpec vsyncSpec = rewrites.get(1).option();
        assertEquals(OptionSpec.Cycling.class, vsyncSpec.getClass());
        OptionSpec.Cycling<?> vsync = (OptionSpec.Cycling<?>) vsyncSpec;
        assertEquals(ActiniumExtraGameOptions.VerticalSyncOption.class, vsync.type());
        assertEquals(new TextSpec.TranslatableKey("options.vsync"), vsync.name());
        assertEquals(OptionSpec.Impact.VARIES, vsync.impact());
        // The control must only offer the modes the driver supports. The supplier is deliberately
        // not called here: it queries GLFW, which needs the game display to exist.
        assertNotNull(vsync.allowedValues(), "the vsync row must restrict its selectable modes");
    }

    @Test
    void everySpecifiedLabelHasAnEnglishTranslation() {
        Set<String> defined = new HashSet<>();
        try (java.io.InputStream in = ActiniumExtraOptionSpecsTest.class.getResourceAsStream(
                "/assets/actiniumextra/lang/en_us.lang")) {
            assertNotNull(in, "en_us.lang is not on the test classpath");
            new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> line.indexOf('=') >= 0 && !line.startsWith("#"))
                    .map(line -> line.substring(0, line.indexOf('=')).trim())
                    .forEach(defined::add);
        } catch (java.io.IOException e) {
            throw new AssertionError("could not read en_us.lang", e);
        }

        List<String> missing = new ArrayList<>();
        for (PageSpec page : ActiniumExtraOptionSpecs.pages()) {
            collectMissingKeys(page.title(), defined, missing);
            page.groups().forEach(group -> {
                group.titleText().ifPresent(title -> collectMissingKeys(title, defined, missing));
                group.options().forEach(option -> {
                    collectMissingKeys(option.name(), defined, missing);
                    collectMissingKeys(option.tooltip(), defined, missing);
                });
            });
        }
        ActiniumExtraOptionSpecs.windowRewrites(true).forEach(rewrite -> {
            collectMissingKeys(rewrite.option().name(), defined, missing);
            collectMissingKeys(rewrite.option().tooltip(), defined, missing);
        });

        assertTrue(missing.isEmpty(), () -> "language keys used by the specs but absent from en_us.lang: "
                + missing);
    }

    private static void collectMissingKeys(TextSpec text, Set<String> defined, List<String> missing) {
        if (text instanceof TextSpec.LiteralKey(String key) && !defined.contains(key)) {
            missing.add(key);
        }
    }

    private static List<StandardRewriteSpec.StandardId> rewriteTargets(boolean provideScreenMode) {
        return ActiniumExtraOptionSpecs.windowRewrites(provideScreenMode).stream()
                .map(StandardRewriteSpec::standard)
                .toList();
    }

    /**
     * Walks a gate tree, asserting every parent reference points at a toggle the adapters convert
     * before this one. Only a toggle can gate a row: the adapters read the parent's value as a
     * boolean, so gating on a slider or a cycle would fail at runtime with a class cast.
     *
     * @param page      the page holding the row
     * @param optionKey the row whose gate is checked
     * @param gate      the gate to walk
     * @param built     the rows converted before this one
     */
    private static void assertGateResolvable(PageSpec page, String optionKey, GateSpec gate,
                                             Map<String, OptionSpec> built) {
        switch (gate) {
            case GateSpec.ByKey(String parentKey) -> {
                OptionSpec parent = built.get(parentKey);
                assertInstanceOf(OptionSpec.Bool.class, parent, () -> page.id().path() + " gates '"
                        + optionKey + "' on '" + parentKey + "', which is not a toggle built before it");
            }
            case GateSpec.All(List<GateSpec> gates) ->
                    gates.forEach(part -> assertGateResolvable(page, optionKey, part, built));
            case GateSpec.Always ignored -> {
                // The always gate depends on no other row, so there is nothing to resolve.
            }
            case GateSpec.Live live -> {
                // A live gate reads an addon-side condition, not another row.
            }
        }
    }

    private static List<String> keys(GroupSpec group) {
        return group.options().stream().map(OptionSpec::nameKey).toList();
    }

    private static List<String> duplicates(List<String> keys) {
        Set<String> seen = new HashSet<>();
        return keys.stream().filter(key -> !seen.add(key)).toList();
    }
}
