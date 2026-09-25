package dev.mxxfoxx.actiniumextra.client.gui.spec;

import dev.mxxfoxx.actiniumextra.ActiniumExtraMod;
import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions;
import dev.mxxfoxx.actiniumextra.client.particle.ParticleClassRegistry;
import dev.mxxfoxx.actiniumextra.client.render.cloud.ModernCloudAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Describes every option page and every WINDOW-group replacement Actinium Extra contributes,
 * without naming a single renderer class.
 * <p>
 * Celeritas and Actinium expose the same option model under different package names, and each
 * renderer only accepts its own types in its settings screen, so the contributions are specified
 * here once and converted by the matching adapter in {@code dev.mxxfoxx.actiniumextra.renderer}.
 * Keeping the description shared is what lets one addon jar serve both renderers without shipping
 * two copies of the page layout that could drift apart.
 * <p>
 * Options are built in display order, and a dependent option must come after the option it gates on
 * ({@link GateSpec#key(String)}), because the adapters resolve gates against the rows they have
 * already converted.
 */
public final class ActiniumExtraOptionSpecs {
    /** Language key of the master animations switch; the per-type switches gate on it. */
    public static final String ANIMATIONS_ALL = "actiniumextra.option.animations.all";
    /** Language key of the master particles switch; the per-class switches gate on it. */
    public static final String PARTICLES_ALL = "actiniumextra.option.particles.all";
    /** Language key of the stars switch; the total-star slider gates on it. */
    public static final String DETAILS_STARS = "actiniumextra.option.details.stars";
    /** Language key of the clouds switch; the cloud sub-options gate on it. */
    public static final String RENDER_CLOUDS = "actiniumextra.option.render.clouds";
    /** Language key of the item-frame switch; frame sub-options gate on it. */
    public static final String RENDER_ITEM_FRAMES = "actiniumextra.option.render.item_frames";
    /** Language key of the beacon switch; the beam-height switch gates on it. */
    public static final String RENDER_BEACONS = "actiniumextra.option.render.beacons";
    /** Language key of the FPS overlay switch; the extended-statistics switch gates on it. */
    public static final String EXTRA_FPS = "actiniumextra.option.extra.fps";
    /** Language key of the steady debug HUD switch; its refresh slider gates on it. */
    public static final String EXTRA_STEADY_DEBUG_HUD = "actiniumextra.option.extra.steady_debug_hud";
    /** Language key of the toast switch; the per-type toast switches gate on it. */
    public static final String EXTRA_TOASTS = "actiniumextra.option.extra.toasts";

    private static final String PARTICLE_PAGE_TITLE = "actiniumextra.option.page.particles";
    private static final String PARTICLE_OTHER_TOOLTIP = "actiniumextra.option.particles.other.tooltip";

    private ActiniumExtraOptionSpecs() {
    }

    /**
     * Builds the five contributed pages in the order they appear in the settings UI.
     *
     * @return the page specifications
     */
    public static List<PageSpec> pages() {
        return List.of(animations(), particles(), details(), render(), extra());
    }

    /**
     * Builds the WINDOW-group replacements for the currently selected renderer.
     * <p>
     * The screen-mode replacement is only contributed when the renderer leaves window management to
     * vanilla: Actinium has its own fullscreen-mode option, and overriding the vanilla toggle
     * alongside it would put two controls in charge of one piece of state. The adaptive-VSync
     * replacement is always contributed, since neither renderer offers adaptive sync.
     *
     * @param provideScreenMode whether the addon should replace the renderer's fullscreen toggle,
     *                          decided by {@link dev.mxxfoxx.actiniumextra.renderer.RendererGlue}
     *                          from the renderer's own capabilities and the current desktop
     * @return the rewrites to apply, in target order
     */
    public static List<StandardRewriteSpec> windowRewrites(boolean provideScreenMode) {
        List<StandardRewriteSpec> rewrites = new ArrayList<>();

        if (provideScreenMode) {
            rewrites.add(new StandardRewriteSpec(StandardRewriteSpec.StandardId.FULLSCREEN,
                    OptionSpec.cycling("actiniumextra.option.screen_mode",
                            ActiniumExtraGameOptions.ScreenMode.class,
                            ActiniumExtraGameOptions.ScreenMode::getLocalizedName,
                            ActiniumExtraGameOptions.ScreenMode::apply,
                            ActiniumExtraGameOptions.ScreenMode::getCurrent)));
        }

        rewrites.add(new StandardRewriteSpec(StandardRewriteSpec.StandardId.VSYNC,
                OptionSpec.cycling("actiniumextra.option.extra.vertical_sync",
                        TextSpec.translatableKey("options.vsync"),
                        TextSpec.literalKey("actiniumextra.option.extra.vertical_sync.tooltip"),
                        ActiniumExtraGameOptions.VerticalSyncOption.class,
                        ActiniumExtraGameOptions.VerticalSyncOption::getLocalizedName,
                        ActiniumExtraGameOptions.VerticalSyncOption::apply,
                        ActiniumExtraGameOptions.VerticalSyncOption::getCurrent,
                        OptionSpec.Impact.VARIES)));

        return List.copyOf(rewrites);
    }

    /**
     * The Animations page: a master toggle gating per-type animation switches (water, lava, fire,
     * portal, and block animations), all flagged to reload assets when changed.
     *
     * @return the animations page specification
     */
    public static PageSpec animations() {
        GateSpec animationsOn = GateSpec.key(ANIMATIONS_ALL);

        return new PageSpec(ActiniumExtraOptionPages.ANIMATION,
                TextSpec.literalKey("actiniumextra.option.page.animations"),
                List.of(
                        GroupSpec.of(OptionSpec.bool(ANIMATIONS_ALL,
                                (opts, v) -> opts.animationSettings.animation = v,
                                opts -> opts.animationSettings.animation,
                                OptionSpec.Flag.REQUIRES_ASSET_RELOAD,
                                OptionSpec.Impact.MEDIUM)),
                        GroupSpec.of(
                                OptionSpec.bool("actiniumextra.option.animations.water",
                                        (opts, v) -> opts.animationSettings.water = v,
                                        opts -> opts.animationSettings.water,
                                        OptionSpec.Flag.REQUIRES_ASSET_RELOAD, animationsOn),
                                OptionSpec.bool("actiniumextra.option.animations.lava",
                                        (opts, v) -> opts.animationSettings.lava = v,
                                        opts -> opts.animationSettings.lava,
                                        OptionSpec.Flag.REQUIRES_ASSET_RELOAD, animationsOn),
                                OptionSpec.bool("actiniumextra.option.animations.fire",
                                        (opts, v) -> opts.animationSettings.fire = v,
                                        opts -> opts.animationSettings.fire,
                                        OptionSpec.Flag.REQUIRES_ASSET_RELOAD, animationsOn),
                                OptionSpec.bool("actiniumextra.option.animations.portal",
                                        (opts, v) -> opts.animationSettings.portal = v,
                                        opts -> opts.animationSettings.portal,
                                        OptionSpec.Flag.REQUIRES_ASSET_RELOAD, animationsOn),
                                OptionSpec.bool("actiniumextra.option.animations.block",
                                        (opts, v) -> opts.animationSettings.blockAnimations = v,
                                        opts -> opts.animationSettings.blockAnimations,
                                        OptionSpec.Flag.REQUIRES_ASSET_RELOAD, animationsOn))));
    }

    /**
     * The Particles page: the master toggle and the built-in particle switches, followed by
     * dynamically discovered per-class toggles grouped by owning mod.
     * <p>
     * Particle-class discovery normally happens at world load; re-scanning here is a defensive
     * supplement, and it is guarded so a discovery failure cannot drop the static switches above it
     * or the later pages.
     *
     * @return the particles page specification
     */
    public static PageSpec particles() {
        GateSpec particlesOn = GateSpec.key(PARTICLES_ALL);
        List<GroupSpec> groups = new ArrayList<>();

        groups.add(GroupSpec.of(OptionSpec.bool(PARTICLES_ALL,
                (opts, v) -> opts.particleSettings.particles = v,
                opts -> opts.particleSettings.particles,
                OptionSpec.Impact.HIGH)));

        groups.add(GroupSpec.of(
                OptionSpec.bool("actiniumextra.option.particles.rain_splash",
                        (opts, v) -> opts.particleSettings.rainSplash = v,
                        opts -> opts.particleSettings.rainSplash, particlesOn),
                OptionSpec.bool("actiniumextra.option.particles.block_break",
                        (opts, v) -> opts.particleSettings.blockBreak = v,
                        opts -> opts.particleSettings.blockBreak, particlesOn),
                OptionSpec.bool("actiniumextra.option.particles.block_breaking",
                        (opts, v) -> opts.particleSettings.blockBreaking = v,
                        opts -> opts.particleSettings.blockBreaking, particlesOn)));

        groups.addAll(discoveredParticleGroups(particlesOn));

        return new PageSpec(ActiniumExtraOptionPages.PARTICLE,
                TextSpec.literalKey(PARTICLE_PAGE_TITLE), List.copyOf(groups));
    }

    /**
     * Builds the per-mod particle toggle groups from the discovery cache, grouped by owning mod and
     * sorted by simple class name.
     *
     * @param particlesOn gate mirroring the master particles switch
     * @return one group per discovered mod, empty when discovery fails
     */
    private static List<GroupSpec> discoveredParticleGroups(GateSpec particlesOn) {
        List<GroupSpec> groups = new ArrayList<>();
        try {
            ParticleClassRegistry registry = ParticleClassRegistry.getInstance();
            registry.scanFactories(Minecraft.getMinecraft().effectRenderer);

            Map<String, String> discovered = registry.getDiscoveredClasses();
            if (discovered.isEmpty()) {
                return List.of();
            }

            Map<String, List<Map.Entry<String, String>>> byMod = new TreeMap<>();
            for (Map.Entry<String, String> entry : discovered.entrySet()) {
                String modId = registry.getModName(entry.getKey());
                byMod.computeIfAbsent(modId == null ? "unknown" : modId, ignored -> new ArrayList<>())
                        .add(entry);
            }

            for (Map.Entry<String, List<Map.Entry<String, String>>> modEntry : byMod.entrySet()) {
                String modId = modEntry.getKey();
                List<Map.Entry<String, String>> classEntries = new ArrayList<>(modEntry.getValue());
                classEntries.sort(Comparator.comparing(Map.Entry::getValue));

                List<OptionSpec> rows = new ArrayList<>();
                for (Map.Entry<String, String> classEntry : classEntries) {
                    String fullClassName = classEntry.getKey();
                    String simpleClassName = classEntry.getValue();
                    rows.add(OptionSpec.bool(fullClassName,
                            TextSpec.literalText(simpleClassName + " (" + modId + ")"),
                            TextSpec.literalText(I18n.format(PARTICLE_OTHER_TOOLTIP, simpleClassName)
                                    + "\n" + fullClassName),
                            (opts, value) -> registry.setClassEnabled(fullClassName, value),
                            opts -> !registry.isClassDisabled(fullClassName),
                            particlesOn));
                }
                groups.add(new GroupSpec(List.copyOf(rows)));
            }
        } catch (Throwable throwable) {
            ActiniumExtraMod.LOGGER.warn("Failed to build dynamic particle toggles", throwable);
        }
        return groups;
    }

    /**
     * The Details page: sky, stars (with a star-count slider), sun/moon, weather, biome and sky
     * colors, and void fog.
     *
     * @return the details page specification
     */
    public static PageSpec details() {
        return new PageSpec(ActiniumExtraOptionPages.DETAILS,
                TextSpec.literalKey("actiniumextra.option.page.details"),
                List.of(GroupSpec.of(
                        OptionSpec.bool("actiniumextra.option.details.sky",
                                (opts, v) -> opts.detailSettings.sky = v,
                                opts -> opts.detailSettings.sky,
                                OptionSpec.Flag.REQUIRES_RENDERER_RELOAD),
                        OptionSpec.bool(DETAILS_STARS,
                                (opts, v) -> opts.detailSettings.stars = v,
                                opts -> opts.detailSettings.stars,
                                OptionSpec.Flag.REQUIRES_RENDERER_RELOAD),
                        OptionSpec.slider("actiniumextra.option.details.total_stars",
                                500, 32000, 500, FormatterSpec.number(),
                                (opts, v) -> opts.detailSettings.totalStars = v,
                                opts -> opts.detailSettings.totalStars,
                                GateSpec.key(DETAILS_STARS), OptionSpec.Impact.MEDIUM,
                                OptionSpec.Flag.REQUIRES_RENDERER_RELOAD),
                        OptionSpec.bool("actiniumextra.option.details.sun_moon",
                                (opts, v) -> opts.detailSettings.sunMoon = v,
                                opts -> opts.detailSettings.sunMoon,
                                OptionSpec.Flag.REQUIRES_RENDERER_RELOAD),
                        OptionSpec.bool("actiniumextra.option.details.rain_snow",
                                (opts, v) -> opts.detailSettings.rainSnow = v,
                                opts -> opts.detailSettings.rainSnow),
                        OptionSpec.bool("actiniumextra.option.details.biome_colors",
                                (opts, v) -> opts.detailSettings.biomeColors = v,
                                opts -> opts.detailSettings.biomeColors,
                                OptionSpec.Flag.REQUIRES_RENDERER_RELOAD),
                        OptionSpec.bool("actiniumextra.option.details.sky_colors",
                                (opts, v) -> opts.detailSettings.skyColors = v,
                                opts -> opts.detailSettings.skyColors),
                        OptionSpec.bool("actiniumextra.option.details.void_fog",
                                (opts, v) -> opts.detailSettings.voidFog = v,
                                opts -> opts.detailSettings.voidFog))));
    }

    /**
     * The Render page: fog, clouds (height, distance, scale, and translucency), light updates, and
     * per-entity/block render toggles with their dependent sliders.
     *
     * @return the render page specification
     */
    public static PageSpec render() {
        GateSpec fogOn = GateSpec.key("actiniumextra.option.render.fog");
        GateSpec cloudsOn = GateSpec.key(RENDER_CLOUDS);

        return new PageSpec(ActiniumExtraOptionPages.RENDER,
                TextSpec.literalKey("actiniumextra.option.page.render"),
                List.of(GroupSpec.of(
                        OptionSpec.bool("actiniumextra.option.render.fog",
                                (opts, v) -> opts.renderSettings.fog = v,
                                opts -> opts.renderSettings.fog),
                        OptionSpec.slider("actiniumextra.option.render.fog_start",
                                0, 200, 10, FormatterSpec.percentage(),
                                (opts, v) -> opts.renderSettings.fogStart = v,
                                opts -> opts.renderSettings.fogStart, fogOn),
                        OptionSpec.slider("actiniumextra.option.render.fog_distance",
                                0, 32, 1, FormatterSpec.quantityOrDisabled("chunks", "Default"),
                                (opts, v) -> opts.renderSettings.fogDistance = v,
                                opts -> opts.renderSettings.fogDistance, fogOn),
                        OptionSpec.bool("actiniumextra.option.render.prevent_shaders",
                                (opts, v) -> opts.renderSettings.preventShaders = v,
                                opts -> opts.renderSettings.preventShaders),
                        OptionSpec.bool(RENDER_CLOUDS,
                                (opts, v) -> opts.renderSettings.clouds = v,
                                opts -> opts.renderSettings.clouds),
                        OptionSpec.bool("actiniumextra.option.render.modern_clouds",
                                (opts, v) -> opts.renderSettings.modernClouds = v,
                                opts -> opts.renderSettings.modernClouds,
                                OptionSpec.Flag.REQUIRES_ASSET_RELOAD,
                                GateSpec.all(cloudsOn, GateSpec.live(ModernCloudAssets::isAvailable))),
                        OptionSpec.slider("actiniumextra.option.render.cloud_height",
                                ActiniumExtraGameOptions.RenderSettings.USE_WORLD_CLOUD_HEIGHT,
                                384, 16,
                                FormatterSpec.custom(v -> v < 0 ? "Default" : v + " blocks"),
                                (opts, v) -> opts.renderSettings.cloudHeight = v,
                                opts -> opts.renderSettings.cloudHeight, cloudsOn),
                        OptionSpec.slider("actiniumextra.option.render.cloud_distance",
                                0, 128, 1, FormatterSpec.quantityOrDisabled("chunks", "Default"),
                                (opts, v) -> opts.renderSettings.cloudDistance = v,
                                opts -> opts.renderSettings.cloudDistance,
                                cloudsOn, OptionSpec.Impact.HIGH),
                        OptionSpec.slider("actiniumextra.option.render.cloud_scale",
                                ActiniumExtraGameOptions.RenderSettings.CLOUD_SCALE_MIN,
                                ActiniumExtraGameOptions.RenderSettings.CLOUD_SCALE_MAX, 1,
                                FormatterSpec.custom(v -> String.format(Locale.ROOT, "%.2fx",
                                        (float) v / ActiniumExtraGameOptions.RenderSettings.CLOUD_SCALE_VANILLA)),
                                (opts, v) -> opts.renderSettings.cloudScale = v,
                                opts -> opts.renderSettings.cloudScale, cloudsOn),
                        OptionSpec.cycling("actiniumextra.option.render.cloud_translucency",
                                ActiniumExtraGameOptions.CloudTranslucency.class,
                                ActiniumExtraGameOptions.CloudTranslucency::getLocalizedName,
                                (opts, value) -> opts.renderSettings.cloudTranslucency = value,
                                opts -> opts.renderSettings.cloudTranslucency, cloudsOn),
                        OptionSpec.bool("actiniumextra.option.render.light_updates",
                                (opts, v) -> opts.renderSettings.lightUpdates = v,
                                opts -> opts.renderSettings.lightUpdates,
                                OptionSpec.Impact.HIGH),
                        OptionSpec.bool(RENDER_ITEM_FRAMES,
                                (opts, v) -> opts.renderSettings.itemFrames = v,
                                opts -> opts.renderSettings.itemFrames),
                        OptionSpec.slider("actiniumextra.option.render.item_frame_lod_distance",
                                0, 256, 1, FormatterSpec.quantityOrDisabled("blocks", "Off"),
                                (opts, v) -> opts.renderSettings.itemFrameLodDistance = v,
                                opts -> opts.renderSettings.itemFrameLodDistance,
                                GateSpec.key(RENDER_ITEM_FRAMES), OptionSpec.Impact.LOW),
                        OptionSpec.bool("actiniumextra.option.render.armor_stands",
                                (opts, v) -> opts.renderSettings.armorStands = v,
                                opts -> opts.renderSettings.armorStands),
                        OptionSpec.bool("actiniumextra.option.render.paintings",
                                (opts, v) -> opts.renderSettings.paintings = v,
                                opts -> opts.renderSettings.paintings),
                        OptionSpec.bool(RENDER_BEACONS,
                                (opts, v) -> opts.renderSettings.beacons = v,
                                opts -> opts.renderSettings.beacons),
                        OptionSpec.bool("actiniumextra.option.render.limit_beacon_beam_height",
                                (opts, v) -> opts.renderSettings.limitBeaconBeamHeight = v,
                                opts -> opts.renderSettings.limitBeaconBeamHeight,
                                GateSpec.key(RENDER_BEACONS)),
                        OptionSpec.bool("actiniumextra.option.render.pistons",
                                (opts, v) -> opts.renderSettings.pistons = v,
                                opts -> opts.renderSettings.pistons),
                        OptionSpec.bool("actiniumextra.option.render.enchanting_books",
                                (opts, v) -> opts.renderSettings.enchantingTableBooks = v,
                                opts -> opts.renderSettings.enchantingTableBooks),
                        OptionSpec.bool("actiniumextra.option.render.player_name_tag",
                                (opts, v) -> opts.renderSettings.playerNameTag = v,
                                opts -> opts.renderSettings.playerNameTag),
                        OptionSpec.bool("actiniumextra.option.render.item_frame_name_tag",
                                (opts, v) -> opts.renderSettings.itemFrameNameTag = v,
                                opts -> opts.renderSettings.itemFrameNameTag,
                                GateSpec.key(RENDER_ITEM_FRAMES)))));
    }

    /**
     * The Extra page: the FPS/coordinate overlay group, miscellaneous quality-of-life settings, and
     * toast toggles.
     *
     * @return the extra page specification
     */
    public static PageSpec extra() {
        return new PageSpec(ActiniumExtraOptionPages.EXTRA,
                TextSpec.literalKey("actiniumextra.option.page.extra"),
                List.of(
                        GroupSpec.of(
                                OptionSpec.bool(EXTRA_FPS,
                                        (opts, v) -> opts.extraSettings.showFps = v,
                                        opts -> opts.extraSettings.showFps),
                                OptionSpec.bool("actiniumextra.option.extra.fps_extended",
                                        (opts, v) -> opts.extraSettings.showFPSExtended = v,
                                        opts -> opts.extraSettings.showFPSExtended,
                                        GateSpec.key(EXTRA_FPS)),
                                OptionSpec.bool("actiniumextra.option.extra.coords",
                                        (opts, v) -> opts.extraSettings.showCoords = v,
                                        opts -> opts.extraSettings.showCoords),
                                OptionSpec.bool("actiniumextra.option.extra.ignore_reduced_debug_info",
                                        (opts, v) -> opts.extraSettings.ignoreReducedDebugInfo = v,
                                        opts -> opts.extraSettings.ignoreReducedDebugInfo),
                                OptionSpec.cycling("actiniumextra.option.extra.overlay_corner",
                                        ActiniumExtraGameOptions.OverlayCorner.class,
                                        ActiniumExtraGameOptions.OverlayCorner::getLocalizedName,
                                        (opts, value) -> opts.extraSettings.overlayCorner = value,
                                        opts -> opts.extraSettings.overlayCorner),
                                OptionSpec.cycling("actiniumextra.option.extra.text_contrast",
                                        ActiniumExtraGameOptions.TextContrast.class,
                                        ActiniumExtraGameOptions.TextContrast::getLocalizedName,
                                        (opts, value) -> opts.extraSettings.textContrast = value,
                                        opts -> opts.extraSettings.textContrast)),
                        GroupSpec.of(
                                OptionSpec.bool("actiniumextra.option.extra.mod_name_tooltip",
                                        (opts, v) -> opts.extraSettings.modNameTooltip = v,
                                        opts -> opts.extraSettings.modNameTooltip),
                                OptionSpec.bool(EXTRA_STEADY_DEBUG_HUD,
                                        (opts, v) -> opts.extraSettings.steadyDebugHud = v,
                                        opts -> opts.extraSettings.steadyDebugHud),
                                OptionSpec.slider("actiniumextra.option.extra.steady_debug_hud_refresh",
                                        ActiniumExtraGameOptions.ExtraSettings.STEADY_DEBUG_HUD_REFRESH_MIN,
                                        ActiniumExtraGameOptions.ExtraSettings.STEADY_DEBUG_HUD_REFRESH_MAX,
                                        1,
                                        FormatterSpec.custom(v -> v + (v == 1 ? " tick" : " ticks")),
                                        (opts, v) -> opts.extraSettings.steadyDebugHudRefreshInterval = v,
                                        opts -> opts.extraSettings.steadyDebugHudRefreshInterval,
                                        GateSpec.key(EXTRA_STEADY_DEBUG_HUD))),
                        GroupSpec.of(
                                OptionSpec.bool(EXTRA_TOASTS,
                                        (opts, v) -> opts.extraSettings.toasts = v,
                                        opts -> opts.extraSettings.toasts),
                                OptionSpec.bool("actiniumextra.option.extra.toast_advancement",
                                        (opts, v) -> opts.extraSettings.toastAdvancement = v,
                                        opts -> opts.extraSettings.toastAdvancement,
                                        GateSpec.key(EXTRA_TOASTS)),
                                OptionSpec.bool("actiniumextra.option.extra.toast_recipe",
                                        (opts, v) -> opts.extraSettings.toastRecipe = v,
                                        opts -> opts.extraSettings.toastRecipe,
                                        GateSpec.key(EXTRA_TOASTS)),
                                OptionSpec.bool("actiniumextra.option.extra.toast_tutorial",
                                        (opts, v) -> opts.extraSettings.toastTutorial = v,
                                        opts -> opts.extraSettings.toastTutorial,
                                        GateSpec.key(EXTRA_TOASTS)),
                                OptionSpec.bool("actiniumextra.option.extra.toast_system",
                                        (opts, v) -> opts.extraSettings.toastSystem = v,
                                        opts -> opts.extraSettings.toastSystem,
                                        GateSpec.key(EXTRA_TOASTS)))));
    }
}
