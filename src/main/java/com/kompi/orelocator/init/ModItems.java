package com.kompi.orelocator.init;

import com.kompi.orelocator.OreLocatorConfig;
import com.kompi.orelocator.RudoLocator;
import com.kompi.orelocator.item.OreLocatorItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item COPPER_ORE_LOCATOR = new OreLocatorItem(
            new Item.Properties().stacksTo(1),
            () -> OreLocatorConfig.getInstance().oreLocatorRadius.copperCurrentRadius,
            0.0f, 1.0f, 0.0f);
    public static final Item IRON_ORE_LOCATOR = new OreLocatorItem(
            new Item.Properties().stacksTo(1),
            () -> OreLocatorConfig.getInstance().oreLocatorRadius.ironCurrentRadius,
            0.7f, 1.0f, 0.7f);
    public static final Item GOLD_ORE_LOCATOR = new OreLocatorItem(
            new Item.Properties().stacksTo(1),
            () -> OreLocatorConfig.getInstance().oreLocatorRadius.goldCurrentRadius,
            1.0f, 0.65f, 0.0f);
    public static final Item DIAMOND_ORE_LOCATOR = new OreLocatorItem(
            new Item.Properties().stacksTo(1),
            () -> OreLocatorConfig.getInstance().oreLocatorRadius.diamondCurrentRadius,
            0.0f, 1.0f, 1.0f);
    public static final Item NETHERITE_ORE_LOCATOR = new OreLocatorItem(
            new Item.Properties().stacksTo(1).fireResistant(),
            () -> OreLocatorConfig.getInstance().oreLocatorRadius.netheriteCurrentRadius,
            0.8f, 0.0f, 1.0f);

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, RudoLocator.id("copper_ore_locator"), COPPER_ORE_LOCATOR);
        Registry.register(BuiltInRegistries.ITEM, RudoLocator.id("iron_ore_locator"), IRON_ORE_LOCATOR);
        Registry.register(BuiltInRegistries.ITEM, RudoLocator.id("gold_ore_locator"), GOLD_ORE_LOCATOR);
        Registry.register(BuiltInRegistries.ITEM, RudoLocator.id("diamond_ore_locator"), DIAMOND_ORE_LOCATOR);
        Registry.register(BuiltInRegistries.ITEM, RudoLocator.id("netherite_ore_locator"), NETHERITE_ORE_LOCATOR);
    }
}