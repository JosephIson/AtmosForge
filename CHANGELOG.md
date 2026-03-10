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

### Fixed
- Division-by-zero in `UpperLevelDynamics` when upper wind magnitude is zero
- Key collision bug in `ClientCloudData` and `ClientStormData` (XOR replaced with OR for 64-bit region key packing)
- Dead code guard branches in `StormCoreSystem` smoothing (conditions were always false against a compile-time constant)
- Removed debug `System.out.println` in `AtmosEngine` that spammed the server log every 10 ticks