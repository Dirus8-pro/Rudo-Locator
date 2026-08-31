package com.kompi.orelocator.init;

import com.kompi.orelocator.Config;
import com.kompi.orelocator.OreLocatorMod;
import com.kompi.orelocator.item.OreLocatorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OreLocatorMod.MODID);

    public static final RegistryObject<Item> COPPER_ORE_LOCATOR = ITEMS.register("copper_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).tab(ModCreativeTabs.ORELOCATOR_TAB), () -> Config.COPPER_CURRENT_RADIUS.get(), 0.0f, 1.0f, 0.0f));

    public static final RegistryObject<Item> IRON_ORE_LOCATOR = ITEMS.register("iron_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).tab(ModCreativeTabs.ORELOCATOR_TAB), () -> Config.IRON_CURRENT_RADIUS.get(), 0.7f, 1.0f, 0.7f));

    public static final RegistryObject<Item> GOLD_ORE_LOCATOR = ITEMS.register("gold_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).tab(ModCreativeTabs.ORELOCATOR_TAB), () -> Config.GOLD_CURRENT_RADIUS.get(), 1.0f, 0.65f, 0.0f));

    public static final RegistryObject<Item> DIAMOND_ORE_LOCATOR = ITEMS.register("diamond_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).tab(ModCreativeTabs.ORELOCATOR_TAB), () -> Config.DIAMOND_CURRENT_RADIUS.get(), 0.0f, 1.0f, 1.0f));

    public static final RegistryObject<Item> NETHERITE_ORE_LOCATOR = ITEMS.register("netherite_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).fireResistant().tab(ModCreativeTabs.ORELOCATOR_TAB), () -> Config.NETHERITE_CURRENT_RADIUS.get(), 0.8f, 0.0f, 1.0f));
}