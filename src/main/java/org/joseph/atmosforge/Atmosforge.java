package org.joseph.atmosforge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.joseph.atmosforge.core.TickHandler;
import org.joseph.atmosforge.network.AtmoNetwork;
import org.slf4j.Logger;

@Mod(Atmosforge.MODID)
public class Atmosforge {

    public static final String MODID = "atmosforge";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Atmosforge(IEventBus modEventBus, ModContainer modContainer) {

        LOGGER.info("AtmosForge initializing...");

        // Register network payloads on the mod event bus
        modEventBus.addListener(AtmoNetwork::register);

        // Register tick handler (core atmospheric brain)
        NeoForge.EVENT_BUS.register(new TickHandler());

        // Register config (keep if you plan to use it)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}