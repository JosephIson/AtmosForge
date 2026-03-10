# AtmosForge

AtmosForge brings the most realistic weather simulation possible to Minecraft. Powered by physically-based atmospheric models including pressure cells, jet streams, Coriolis forcing, frontal systems, cyclone lifecycles, and convective storm classification — every region of your world experiences dynamic, evolving weather driven by real atmospheric science.

## Features

- **Pressure cells** — per-region surface pressure that diffuses, rises, and falls based on temperature and upper-level dynamics
- **Jet stream & upper-level dynamics** — a planetary-wave jet stream seeds upper winds each tick; thermal wind coupling blends jet flow with temperature gradients to produce realistic upper divergence
- **Coriolis deflection** — surface winds are deflected by hemisphere-aware Coriolis forcing
- **Season model** — axial-tilt-based seasonal cycle drives temperature and moisture variation across latitudes
- **Frontal systems & cyclone lifecycle** — baroclinic instability spawns cyclones that develop, mature, occlude, and decay
- **Precipitation & instability** — diagnostic precipitation model weighing frontal lift, upper divergence, instability index, and moisture
- **Storm classification** — regions are scored each tick and classified as Showers, Thunderstorms, MCS, Supercell, or Extratropical Cyclone
- **Cloud rendering** — translucent cloud layer rendered client-side from server-synced cloudiness data
- **Storm cloud renderer** — visual storm overlays driven by synced storm intensity and type

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| Java | 21+ |

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/`.

## Running in dev

```bash
# Client
./gradlew runClient

# Server
./gradlew runServer
```

## Project Structure

```
src/main/java/org/joseph/atmosforge/
├── atmosphere/   # Physical models (pressure, wind, jet stream, seasons, precipitation, cyclones)
├── client/       # Client-side data stores and renderers
├── core/         # Engine tick loop, config, saved data, world data manager
├── data/         # Climate grid and region coordinate types
├── network/      # Server→client sync payloads
├── ocean/        # Ocean heat system
├── storm/        # Storm cell lifecycle, registry, classification, weather application
└── util/         # Math and noise utilities
```

## License

All Rights Reserved.