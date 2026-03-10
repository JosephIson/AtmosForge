package org.joseph.atmosforge.storm;

import org.joseph.atmosforge.atmosphere.PressureCell;
import org.joseph.atmosforge.core.AtmoConfig;
import org.joseph.atmosforge.data.RegionPos;

import java.util.Map;
import java.util.Random;

/**
 * Server-side tornado lifecycle manager.
 *
 * Each simulation tick this class:
 *   1. Ages and advances existing tornados through FORMING → MATURE → DISSIPATING.
 *   2. Drifts mature tornados with their parent supercell's surface wind.
 *   3. Attempts probabilistic spawning of new tornados from eligible supercells.
 *   4. Removes tornados whose intensity has decayed to zero.
 *
 * Spawn criteria (all must be met):
 *   - Region classified as SUPERCELL with intensity >= STORM_ALIVE_THRESHOLD
 *   - Supercell age >= TORNADO_MIN_SUPERCELL_AGE ticks
 *   - Normalised shear   >= TORNADO_SHEAR_MIN
 *   - Normalised instability >= TORNADO_INSTABILITY_MIN
 *   - Updraft index     >= TORNADO_UPDRAFT_MIN
 *   - No existing tornado already tied to this region
 *   - Random chance roll <= TORNADO_SPAWN_CHANCE per call
 */
public final class TornadoLifecycleModel {

    private static final Random RNG = new Random();

    private TornadoLifecycleModel() {}

    public static void apply(Map<RegionPos, PressureCell> regions,
                             StormRegistry stormRegistry,
                             TornadoRegistry tornadoRegistry) {

        // --- 1. Update existing tornados ---
        for (TornadoCell tornado : tornadoRegistry.getAll()) {

            tornado.incrementAge();

            StormCell parent = stormRegistry.get(tornado.getParentRegion());
            PressureCell cell = regions.get(tornado.getParentRegion());

            boolean parentAlive = parent != null
                    && parent.getType() == StormType.SUPERCELL
                    && parent.getIntensity() >= AtmoConfig.STORM_ALIVE_THRESHOLD;

            switch (tornado.getStage()) {

                case FORMING -> {
                    // Ramp intensity from 0 → 1 linearly over FORMING ticks
                    double ramp = (double) tornado.getAgeTicks() / AtmoConfig.TORNADO_FORMING_TICKS;
                    tornado.setIntensity(Math.min(ramp, 1.0));

                    if (tornado.getAgeTicks() >= AtmoConfig.TORNADO_FORMING_TICKS)
                        tornado.setStage(TornadoCell.Stage.MATURE);

                    if (!parentAlive)
                        tornado.setStage(TornadoCell.Stage.DISSIPATING);
                }

                case MATURE -> {
                    tornado.setIntensity(1.0);

                    int matureEnd = AtmoConfig.TORNADO_FORMING_TICKS + AtmoConfig.TORNADO_MATURE_TICKS;
                    if (tornado.getAgeTicks() >= matureEnd || !parentAlive)
                        tornado.setStage(TornadoCell.Stage.DISSIPATING);

                    // Drift with parent storm surface wind
                    if (cell != null) {
                        tornado.setWorldX(tornado.getWorldX()
                                + cell.getSurfaceWind().getX() * AtmoConfig.TORNADO_DRIFT_SPEED);
                        tornado.setWorldZ(tornado.getWorldZ()
                                + cell.getSurfaceWind().getZ() * AtmoConfig.TORNADO_DRIFT_SPEED);
                    }
                }

                case DISSIPATING -> {
                    tornado.setIntensity(tornado.getIntensity() - AtmoConfig.TORNADO_DISSIPATE_RATE);
                }
            }
        }

        // --- 2. Remove dead tornados ---
        tornadoRegistry.removeIf(t ->
                t.getStage() == TornadoCell.Stage.DISSIPATING && t.getIntensity() <= 0.0);

        // --- 3. Try to spawn new tornados ---
        for (Map.Entry<RegionPos, StormCell> e : stormRegistry.getAll().entrySet()) {

            StormCell storm = e.getValue();

            if (storm.getType() != StormType.SUPERCELL) continue;
            if (storm.getIntensity() < AtmoConfig.STORM_ALIVE_THRESHOLD) continue;
            if (storm.getAgeTicks() < AtmoConfig.TORNADO_MIN_SUPERCELL_AGE) continue;

            PressureCell cell = regions.get(e.getKey());
            if (cell == null) continue;

            double shear = cell.getShearIndex() / AtmoConfig.SHEAR_NORM;
            double instability = cell.getInstabilityIndex() / AtmoConfig.INSTABILITY_NORM;
            double updraft = cell.getUpdraftIndex();

            if (shear < AtmoConfig.TORNADO_SHEAR_MIN) continue;
            if (instability < AtmoConfig.TORNADO_INSTABILITY_MIN) continue;
            if (updraft < AtmoConfig.TORNADO_UPDRAFT_MIN) continue;

            RegionPos pos = e.getKey();

            // One tornado per active supercell region at a time
            boolean alreadyPresent = tornadoRegistry.getAll().stream()
                    .anyMatch(t -> t.getParentRegion().equals(pos));
            if (alreadyPresent) continue;

            if (RNG.nextDouble() > AtmoConfig.TORNADO_SPAWN_CHANCE) continue;

            // Spawn at region center with slight random jitter
            int regionBlockX = pos.x() << 8;   // region → blocks (256-block regions)
            int regionBlockZ = pos.z() << 8;
            double spawnX = regionBlockX + 128 + (RNG.nextDouble() - 0.5) * 80;
            double spawnZ = regionBlockZ + 128 + (RNG.nextDouble() - 0.5) * 80;

            tornadoRegistry.add(new TornadoCell(spawnX, spawnZ, pos));
        }
    }
}