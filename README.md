# Actinium Extra

Actinium Extra is an unofficial client-side add-on for [Celeritas](https://github.com/kappa-maintainer/Celeritas-auto-build/releases) and [Actinium](https://github.com/DHJComical/Actinium), built to run on Cleanroom. It adds more graphics, particle, HUD, and window settings to the renderer's video settings screen.

The mod started as a port of features from Sodium Extra and Rubidium/Embeddium Extra. It also includes several additions and backports made specifically for the Cleanroom environment.

It was previously released as **Celeritas Extra**; the project keeps working with Celeritas and was renamed because it now attaches to Actinium as well. This repository is a maintained fork: it is based on [Sumire Labs / Celeritas-Extra](https://github.com/Sumire-Labs/Celeritas-Extra) by s12kuma01, and continues from there under the `dev.mxxfoxx` Java namespace (the original `jp.s12kuma01` packages no longer apply).

### Upgrading from Celeritas Extra

The mod id changed from `celeritasextra` to `actiniumextra`, so the game treats it as a new mod:

- Rename `config/celeritas-extra.cfg` to `config/actinium-extra.cfg` to keep your settings; the option keys inside are unchanged.
- The Modern Clouds texture is requested from AssetMover under the new `actiniumextra` namespace and is downloaded again once.

## Requirements

- [Cleanroom Loader](https://github.com/CleanroomMC/Cleanroom) 0.6.10-alpha or newer
- One of:
  - [Celeritas](https://github.com/kappa-maintainer/Celeritas-auto-build/releases) 2.4.0 or newer
  - [Actinium](https://github.com/DHJComical/Actinium) alpha-0.0.10 or newer

### Optional dependency

- [AssetMover](https://github.com/CleanroomMC/AssetMover) 2.5 or newer unlocks Modern Clouds.

## Renderer support

Celeritas and Actinium expose the same option model under different package names, and each renderer only accepts its own classes in its settings screen. Actinium Extra therefore describes the pages it contributes once and converts them with a small adapter per renderer, picking the adapter from the mod that is actually installed. One jar covers both.

The layouts are not identical, deliberately:

- Actinium already offers its own windowed / borderless / exclusive fullscreen option, so Actinium Extra does not replace the fullscreen toggle there. On Celeritas, where vanilla's boolean toggle is all there is, the add-on still swaps it for its three-way screen mode control on desktops that can run borderless.
- The adaptive VSync replacement applies to both renderers, as neither offers adaptive sync.

If neither renderer is installed the mod logs an error during startup and contributes no options: the settings live in the renderer's video settings screen, so there is nowhere to reach them from.

## Building

The renderer adapters are compiled into every build, so both renderer API jars must be on the compile classpath. Place them in `libs/` (the Celeritas jar is committed; the Actinium jar is not):

```bash
version=$(grep -m1 '^actinium_api_version *=' gradle.properties | cut -d= -f2 | tr -d ' ')
curl -L -o "libs/actinium-${version}.jar" \
  "https://github.com/DHJComical/Actinium/releases/download/${version}/Actinium-${version}.jar"
```

Then run `./gradlew build`. Bumping the pinned `actinium_api_version` in `gradle.properties` is the only step needed to compile against a newer Actinium release.

To try either renderer in a dev run, put a runnable copy of it into `run/client/mods`; that is also what selects the renderer adapter at runtime.

## Testing

`./gradlew test` runs the unit tests, including the checks that pin the contributed option pages, so a page layout change has to be deliberate. Renderer attachment itself is verified in game: open the video settings and confirm the five Actinium Extra pages appear, and on Actinium that the fullscreen row is still Actinium's own window-mode option.

The two renderer adapters are intentionally near-identical code bound to two different option models, so the unit tests guard the shared page description rather than the adapters: building pages needs the client-side translation bootstrap (`I18n`), which a plain JVM test does not have. When editing one adapter, mirror the change in the other and compare them with their backend names normalized - the only intended difference is `hasWindowModeOption()`.

## Features

- Animation controls for water, lava, fire, portals, and block textures.
- Global and per-type particle controls, including options for rain splashes and block particles. Vanilla and modded particle classes are discovered automatically and cached between launches.
- Controls for the sky, stars and star count, the sun and moon, weather, biome colors, sky colors, and void fog.
- Fog distance and start controls. Blindness, underwater, and lava fog are kept even when normal fog is disabled.
- Cloud height, distance, scale (0.25x to 4.00x), and translucency controls, plus the optional Minecraft 1.21.6 cloud texture downloaded by AssetMover. Modern Clouds is disabled by default and uses a built-in resource pack with the existing renderer. User resource packs take priority; height, distance, and scale remain controlled by the existing Forge cloud settings.
- MoreCulling-style item frame LOD for large frame or map walls. It is disabled by default.
- Render toggles for item frames, armor stands, paintings, pistons, beacon beams, enchanting table books, name tags, light updates, and vanilla screen shaders.
- FPS and coordinate overlays with configurable position and text contrast. Extended FPS statistics include average FPS, 1% low, and 0.1% low.
- Mod-name tooltips, per-type toast controls, a steady F3 debug screen, and renderer names in the F3 profiler pie chart.
- Windowed, borderless, and fullscreen modes, plus Off, On, and Adaptive VSync.

## Credits

- Mxx Foxx, current maintainer
- s12kuma01 and Sumire Labs, who wrote the original Celeritas Extra
- FlashyReese, creator of Sodium Extra
- dima_dencep, creator of Rubidium and Embeddium Extra
- embeddedt, creator of Celeritas
- DHJComical and the Actinium contributors, whose renderer this add-on also attaches to
- CleanroomMC, for Cleanroom Loader, CleanroomModTemplate, and related tools
- Everyone who has contributed translations

## License

Actinium Extra is licensed under the [LGPL-3.0](LICENSE.md).

## AI usage

This project is developed with AI coding agents, and their role is stated here rather than implied:

- Code, tests and documentation in this fork — the dual-renderer support, the option-page
  specification the renderers are adapted to, and the rename that came with it — were generated by an
  AI agent working to the maintainer's instructions.
- The maintainer reviews the changes, runs the build and the unit tests, and decides what is
  committed; the agent's claims are not taken on trust.
- That is a review, not a proof of correctness. Behaviour that only shows up in game (rendering
  regressions, shader-pack and mod interactions) is still verified by hand, so treat anything not
  covered by the test suite as unverified until someone reports otherwise.

Upstream's original code predates this workflow and is credited above.
