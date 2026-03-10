package org.joseph.atmosforge.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.server.level.ServerLevel;
import org.joseph.atmosforge.Config;

public class TickHandler {

    private static final AtmosEngine ENGINE = new AtmosEngine();
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        tickCounter++;

        if (tickCounter >= Config.SIMULATION_INTERVAL.get()) {
            tickCounter = 0;
            ENGINE.tick(level);
        }
    }
}