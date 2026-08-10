package com.kompi.orelocator;

import com.kompi.orelocator.init.ModCreativeTabs;
import com.kompi.orelocator.init.ModItems;
import com.kompi.orelocator.network.ModNetwork;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RudoLocator implements ModInitializer {
	public static final String MODID = "rudo-locator";
	public static final Logger LOGGER = LogManager.getLogger();

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MODID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.register();
		ModCreativeTabs.register();
		ModNetwork.init();      // скоро перепишем
		//Config.register();       // скоро перепишем
	}
}