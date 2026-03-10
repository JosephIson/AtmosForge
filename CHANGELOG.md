# Changelog

All notable changes to AtmosForge will be documented here.

## [Unreleased] - 1.0-SNAPSHOT

### Added
- Pressure cell simulation with per-region surface pressure, temperature, and diffusion
- Jet stream model with planetary-wave steering and per-tick evolution
- Upper-level dynamics: thermal wind coupling, upper divergence, and surface pressure forcing
- Coriolis deflection of surface winds based on hemisphere
- Season model driven by axial tilt across a configurable year length
- Seasonal moisture variation by latitude and hemisphere
- Baroclinic instability and frontal system generation
- Cyclone lifecycle model: development, maturity, occlusion, and decay
- Instability index and diagnostic precipitation model
- Storm classification system: Showers, Thunderstorms, MCS, Supercell, Extratropical Cyclone
- Storm persistence and hysteresis to prevent rapid intensity collapse
- Client-side cloud layer renderer using synced cloudiness data
- Storm cloud renderer driven by storm type and intensity
- Server→client network sync for cloud and storm data
- Climate grid with per-region state persistence via SavedData

- Volumetric cloud deck: `CloudLayerRenderer` now renders 7 stacked horizontal slabs with a bell-curve alpha profile, giving the cloud layer genuine depth instead of a flat painted ceiling
- Per-layer UV offset in volumetric clouds breaks texture repetition when looking up through the cloud mass
- Storm-aware cloud rendering: SUPERCELL, MCS, and THUNDERSTORM regions lower the cloud base and thicken/darken the deck proportional to storm intensity
- `TornadoCell`: data class tracking world position, intensity (0–1), lifecycle stage, and parent supercell region
- `TornadoRegistry`: per-world list of active tornado vortices
- `TornadoLifecycleModel`: spawns tornadoes from supercells exceeding shear/instability/updraft thresholds; advances FORMING → MATURE → DISSIPATING; drifts with parent surface wind
- `TornadoDataPayload` + `ClientTornadoData`: server→client sync pipeline for tornado state
- `TornadoRenderer`: twisted funnel cone (14 rings × 24 angular segments) with client-side rotation, colour grading grey→brown toward the tip, and a swirling debris disk at the funnel base
- `AtmoConfig`: constants for volumetric cloud geometry and all tornado lifecycle parameters

### Fixed
- Division-by-zero in `UpperLevelDynamics` when upper wind magnitude is zero
- Key collision bug in `ClientCloudData` and `ClientStormData` (XOR replaced with OR for 64-bit region key packing)
- Dead code guard branches in `StormCoreSystem` smoothing (conditions were always false against a compile-time constant)
- Removed debug `System.out.println` in `AtmosEngine` that spammed the server log every 10 ticks
- Texture `cloud_noise.png` was in a misnamed flat folder (`assets.atmosforge.textures/`) instead of the correct resource pack path (`assets/atmosforge/textures/`), causing a failed-to-load-texture error at runtime