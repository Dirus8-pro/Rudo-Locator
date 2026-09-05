package com.kompi.orelocator.init;

import com.kompi.orelocator.Config;
import com.kompi.orelocator.OreLocatorMod;
import com.kompi.orelocator.item.OreLocatorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(OreLocatorMod.MODID);

    public static final DeferredItem<Item> COPPER_ORE_LOCATOR = ITEMS.register("copper_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1), () -> Config.COPPER_CURRENT_RADIUS.get(), 0.0f, 1.0f, 0.0f));

    public static final DeferredItem<Item> IRON_ORE_LOCATOR = ITEMS.register("iron_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1), () -> Config.IRON_CURRENT_RADIUS.get(), 0.7f, 1.0f, 0.7f));

    public static final DeferredItem<Item> GOLD_ORE_LOCATOR = ITEMS.register("gold_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1), () -> Config.GOLD_CURRENT_RADIUS.get(), 1.0f, 0.65f, 0.0f));

    public static final DeferredItem<Item> DIAMOND_ORE_LOCATOR = ITEMS.register("diamond_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1), () -> Config.DIAMOND_CURRENT_RADIUS.get(), 0.0f, 1.0f, 1.0f));

    public static final DeferredItem<Item> NETHERITE_ORE_LOCATOR = ITEMS.register("netherite_ore_locator",
            () -> new OreLocatorItem(new Item.Properties().stacksTo(1).fireResistant(), () -> Config.NETHERITE_CURRENT_RADIUS.get(), 0.8f, 0.0f, 1.0f));
}