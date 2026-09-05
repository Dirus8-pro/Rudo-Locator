package com.kompi.orelocator;

import com.kompi.orelocator.init.ModCreativeTabs;
import com.kompi.orelocator.init.ModItems;
import com.kompi.orelocator.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(OreLocatorMod.MODID)
public class OreLocatorMod {
    public static final String MODID = "orelocator";
    public static final Logger LOGGER = LogManager.getLogger();

    public OreLocatorMod(IEventBus modEventBus, ModContainer modContainer) {
        // Регистрация реестров (DeferredRegister) на шине мода
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // Регистрация конфига
        Config.register(modContainer);

        // Регистрация событий игры (глобальная шина)

    }
}