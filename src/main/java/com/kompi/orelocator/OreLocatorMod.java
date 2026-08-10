package com.kompi.orelocator;

import com.kompi.orelocator.init.ModCreativeTabs;
import com.kompi.orelocator.init.ModItems;
import com.kompi.orelocator.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(OreLocatorMod.MODID)
public class OreLocatorMod {
    public static final String MODID = "orelocator";
    public static final Logger LOGGER = LogManager.getLogger();

    public OreLocatorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModNetwork.init();
        Config.register();
        MinecraftForge.EVENT_BUS.register(this);
    }
}