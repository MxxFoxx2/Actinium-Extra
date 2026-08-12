# Celeritas Extra

Celeritas Extra is an unofficial client-side add-on for [Celeritas](https://github.com/kappa-maintainer/Celeritas-auto-build/releases) on Minecraft 1.12.2. It adds more graphics, particle, HUD, and window settings to the Celeritas video settings screen.

The mod started as a port of features from Sodium Extra and Rubidium/Embeddium Extra. It also includes several additions and backports made specifically for the Cleanroom 1.12.2 environment.

## Requirements

- [Cleanroom Loader](https://github.com/CleanroomMC/Cleanroom) 0.6.10-alpha or newer
- [Celeritas](https://github.com/kappa-maintainer/Celeritas-auto-build/releases) 2.4.0 or newer

### Optional dependency

- [AssetMover](https://github.com/CleanroomMC/AssetMover) 2.5 or newer unlocks Modern Clouds.

## Features

- Animation controls for water, lava, fire, portals, and block textures.
- Global and per-type particle controls, including options for rain splashes and block particles. Vanilla and modded particle classes are discovered automatically and cached between launches.
- Controls for the sky, stars and star count, the sun and moon, weather, biome colors, sky colors, and void fog.
- Fog distance and start controls. Blindness, underwater, and lava fog are kept even when normal fog is disabled.
- Cloud height, distance, scale (0.25x to 4.00x), and translucency controls, plus optional Minecraft 1.21.6-style clouds with the updated pattern, circular horizon-scale rendering, and a distance fade. Modern Clouds is disabled by default and requires AssetMover.
- MoreCulling-style item frame LOD for large frame or map walls. It is disabled by default.
- Render toggles for item frames, armor stands, paintings, pistons, beacon beams, enchanting table books, name tags, light updates, and vanilla screen shaders.
- FPS and coordinate overlays with configurable position and text contrast. Extended FPS statistics include average FPS, 1% low, and 0.1% low.
- Mod-name tooltips, per-type toast controls, a steady F3 debug screen, and renderer names in the F3 profiler pie chart.
- Windowed, borderless, and fullscreen modes, plus Off, On, and Adaptive VSync.

## Credits

- FlashyReese, creator of Sodium Extra
- dima_dencep, creator of Rubidium and Embeddium Extra
- embeddedt, creator of Celeritas
- CleanroomMC, for Cleanroom Loader, CleanroomModTemplate, and related tools
- Everyone who has contributed translations

## License

Celeritas Extra is licensed under the [LGPL-3.0](LICENSE.md).

## AI usage

Some code in this project was written with AI assistance. Changes are reviewed before they are included.
