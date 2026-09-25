package jp.s12kuma01.celeritasextra.client.gui.spec;

import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the contributed option layout, so the renderer adapters cannot drift the pages away from
 * what Celeritas Extra has always shown.
 * <p>
 * The adapters build their rows strictly in specification order and resolve a
 * {@link GateSpec.ByKey} against the rows they converted before, so a page whose keys or order
 * change without a matching expectation here breaks both renderers, not just one of them.
 */
class CeleritasExtraOptionSpecsTest {
    private static final String ANIMATIONS = "celeritasextra.option.animations.";
    private static final String PARTICLES = "celeritasextra.option.particles.";
    private static final String DETAILS = "celeritasextra.option.details.";
    private static final String RENDER = "celeritasextra.option.render.";
    private static final String EXTRA = "celeritasextra.option.extra.";

    @Test
    void contributesTheFivePagesInTheirTabOrder() {
        List<PageSpec> pages = CeleritasExtraOptionSpecs.pages();

        assertEquals(5, pages.size());
        assertEquals(List.of("animation", "particle", "details", "render", "extra"),
                pages.stream().map(page -> page.id().path()).toList());
        assertTrue(pages.stream().allMatch(page -> "celeritasextra".equals(page.id().namespace())),
                "Every contributed page must stay in the addon's own namespace");
    }

    @Test
    void animationsPageGatesEveryTypeOnTheMasterSwitch() {
        PageSpec page = CeleritasExtraOptionSpecs.animations();

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
        PageSpec page = CeleritasExtraOptionSpecs.particles();

        assertEquals(List.of(PARTICLES + "all"), keys(page.groups().get(0)));
        assertEquals(List.of(PARTICLES + "rain_splash", PARTICLES + "block_break",
                PARTICLES + "block_breaking"), keys(page.groups().get(1)));

        OptionSpec.Bool master = assertInstanceOf(OptionSpec.Bool.class, page.groups().get(0).options().get(0));
        assertEquals(OptionSpec.Impact.HIGH, master.impact());
        assertEquals(OptionSpec.Flag.NONE, master.flag());
    }

    @Test
    void detailsPageKeepsItsToggleOrderAndStarSlider() {
        PageSpec page = CeleritasExtraOptionSpecs.details();

        assertEquals(List.of(DETAILS + "sky", DETAILS + "stars", DETAILS + "total_stars",
                DETAILS + "sun_moon", DETAILS + "rain_snow", DETAILS + "biome_colors",
                DETAILS + "sky_colors", DETAILS + "void_fog"), keys(page.groups().get(0)));
        assertEquals(1, page.groups().size());

        OptionSpec.Slider stars = assertInstanceOf(OptionSpec.Slider.class, page.groups().get(0).options().get(2));
        assertEquals(500, stars.min());
        assertEquals(32000, stars.max());
        assertEquals(500, stars.step());
        assertEquals(new GateSpec.ByKey(DETAILS + "stars"), stars.gate());
        assertEquals(OptionSpec.Impact.MEDIUM, stars.impact());
        assertEquals(OptionSpec.Flag.REQUIRES_RENDERER_RELOAD, stars.flag());
    }

    @Test
    void renderPageKeepsItsSingleGroupOfFogCloudAndEntityControls() {
        PageSpec page = CeleritasExtraOptionSpecs.render();

        assertEquals(List.of(RENDER + "fog", RENDER + "fog_start", RENDER + "fog_distance",
                RENDER + "prevent_shaders", RENDER + "clouds", RENDER + "modern_clouds",
                RENDER + "cloud_height", RENDER + "cloud_distance", RENDER + "cloud_scale",
                RENDER + "cloud_translucency", RENDER + "light_updates", RENDER + "item_frames",
                RENDER + "item_frame_lod_distance", RENDER + "armor_stands", RENDER + "paintings",
                RENDER + "beacons", RENDER + "limit_beacon_beam_height", RENDER + "pistons",
                RENDER + "enchanting_books", RENDER + "player_name_tag", RENDER + "item_frame_name_tag"),
                keys(page.groups().get(0)));
        assertEquals(1, page.groups().size());

        List<OptionSpec> rows = page.groups().get(0).options();
        OptionSpec.Bool modernClouds = assertInstanceOf(OptionSpec.Bool.class, rows.get(5));
        assertEquals(OptionSpec.Flag.REQUIRES_ASSET_RELOAD, modernClouds.flag());
        GateSpec.All gate = assertInstanceOf(GateSpec.All.class, modernClouds.gate());
        assertEquals(List.of(new GateSpec.ByKey(RENDER + "clouds")),
                gate.gates().stream().filter(GateSpec.ByKey.class::isInstance).toList());
        assertEquals(1, gate.gates().stream().filter(GateSpec.Live.class::isInstance).count());

        OptionSpec.Slider cloudHeight = assertInstanceOf(OptionSpec.Slider.class, rows.get(6));
        assertEquals(CeleritasExtraGameOptions.RenderSettings.USE_WORLD_CLOUD_HEIGHT, cloudHeight.min());
        assertEquals(384, cloudHeight.max());

        OptionSpec.Slider cloudScale = assertInstanceOf(OptionSpec.Slider.class, rows.get(8));
        assertEquals(CeleritasExtraGameOptions.RenderSettings.CLOUD_SCALE_MIN, cloudScale.min());
        assertEquals(CeleritasExtraGameOptions.RenderSettings.CLOUD_SCALE_MAX, cloudScale.max());
    }

    @Test
    void extraPageKeepsItsOverlayMiscellaneousAndToastGroups() {
        PageSpec page = CeleritasExtraOptionSpecs.extra();

        assertEquals(List.of(EXTRA + "fps", EXTRA + "fps_extended", EXTRA + "coords",
                EXTRA + "ignore_reduced_debug_info", EXTRA + "overlay_corner", EXTRA + "text_contrast"),
                keys(page.groups().get(0)));
        assertEquals(List.of(EXTRA + "mod_name_tooltip", EXTRA + "steady_debug_hud",
                EXTRA + "steady_debug_hud_refresh"), keys(page.groups().get(1)));
        assertEquals(List.of(EXTRA + "toasts", EXTRA + "toast_advancement", EXTRA + "toast_recipe",
                EXTRA + "toast_tutorial", EXTRA + "toast_system"), keys(page.groups().get(2)));

        OptionSpec cornerSpec = page.groups().get(0).options().get(4);
        assertEquals(OptionSpec.Cycling.class, cornerSpec.getClass());
        OptionSpec.Cycling<?> corner = (OptionSpec.Cycling<?>) cornerSpec;
        assertEquals(CeleritasExtraGameOptions.OverlayCorner.class, corner.type());
    }

    @Test
    void everyGateTargetsAnOptionTheAdaptersHaveAlreadyBuilt() {
        for (PageSpec page : CeleritasExtraOptionSpecs.pages()) {
            Set<String> built = new HashSet<>();
            for (GroupSpec group : page.groups()) {
                for (OptionSpec option : group.options()) {
                    assertGateResolvable(page, option.nameKey(), option.gate(), built);
                    built.add(option.nameKey());
                }
            }
        }
    }

    @Test
    void optionKeysAreUniqueAcrossTheWholeContribution() {
        List<String> keys = new ArrayList<>();
        for (PageSpec page : CeleritasExtraOptionSpecs.pages()) {
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
        List<StandardRewriteSpec> rewrites = CeleritasExtraOptionSpecs.windowRewrites(true);

        OptionSpec screenModeSpec = rewrites.get(0).option();
        assertEquals(OptionSpec.Cycling.class, screenModeSpec.getClass());
        OptionSpec.Cycling<?> screenMode = (OptionSpec.Cycling<?>) screenModeSpec;
        assertEquals("celeritasextra.option.screen_mode", screenMode.nameKey());
        assertEquals(new TextSpec.LiteralKey("celeritasextra.option.screen_mode"), screenMode.name());
        assertEquals(CeleritasExtraGameOptions.ScreenMode.class, screenMode.type());

        OptionSpec vsyncSpec = rewrites.get(1).option();
        assertEquals(OptionSpec.Cycling.class, vsyncSpec.getClass());
        OptionSpec.Cycling<?> vsync = (OptionSpec.Cycling<?>) vsyncSpec;
        assertEquals(CeleritasExtraGameOptions.VerticalSyncOption.class, vsync.type());
        assertEquals(new TextSpec.TranslatableKey("options.vsync"), vsync.name());
        assertEquals(OptionSpec.Impact.VARIES, vsync.impact());
    }

    private static List<StandardRewriteSpec.StandardId> rewriteTargets(boolean provideScreenMode) {
        return CeleritasExtraOptionSpecs.windowRewrites(provideScreenMode).stream()
                .map(StandardRewriteSpec::standard)
                .toList();
    }

    /**
     * Walks a gate tree, asserting every parent reference points at a row the adapters convert
     * before this one.
     *
     * @param page      the page holding the row
     * @param optionKey the row whose gate is checked
     * @param gate      the gate to walk
     * @param built     the rows converted before this one
     */
    private static void assertGateResolvable(PageSpec page, String optionKey, GateSpec gate,
                                             Set<String> built) {
        switch (gate) {
            case GateSpec.ByKey(String parentKey) -> assertTrue(built.contains(parentKey),
                    () -> page.id().path() + " gates '" + optionKey + "' on '" + parentKey
                            + "', which is not built before it");
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
