package com.kompi.orelocator;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = OreLocatorMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ModConfigSpec SPEC;
    private static ModConfig configInstance;

    public static ModConfigSpec.IntValue COPPER_MAX_RADIUS;
    public static ModConfigSpec.IntValue IRON_MAX_RADIUS;
    public static ModConfigSpec.IntValue GOLD_MAX_RADIUS;
    public static ModConfigSpec.IntValue DIAMOND_MAX_RADIUS;
    public static ModConfigSpec.IntValue NETHERITE_MAX_RADIUS;

    public static ModConfigSpec.IntValue COPPER_CURRENT_RADIUS;
    public static ModConfigSpec.IntValue IRON_CURRENT_RADIUS;
    public static ModConfigSpec.IntValue GOLD_CURRENT_RADIUS;
    public static ModConfigSpec.IntValue DIAMOND_CURRENT_RADIUS;
    public static ModConfigSpec.IntValue NETHERITE_CURRENT_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("ore_locator_radius");

        COPPER_MAX_RADIUS = builder.comment("Maximum radius for Copper Ore Locator").defineInRange("copper_max_radius", 8, 1, 256);
        IRON_MAX_RADIUS = builder.comment("Maximum radius for Iron Ore Locator").defineInRange("iron_max_radius", 12, 1, 256);
        GOLD_MAX_RADIUS = builder.comment("Maximum radius for Gold Ore Locator").defineInRange("gold_max_radius", 24, 1, 256);
        DIAMOND_MAX_RADIUS = builder.comment("Maximum radius for Diamond Ore Locator").defineInRange("diamond_max_radius", 40, 1, 256);
        NETHERITE_MAX_RADIUS = builder.comment("Maximum radius for Netherite Ore Locator").defineInRange("netherite_max_radius", 64, 1, 256);

        COPPER_CURRENT_RADIUS = builder.comment("Current radius (can be changed in GUI)").defineInRange("copper_current_radius", 8, 1, 256);
        IRON_CURRENT_RADIUS = builder.comment("Current radius (can be changed in GUI)").defineInRange("iron_current_radius", 12, 1, 256);
        GOLD_CURRENT_RADIUS = builder.comment("Current radius (can be changed in GUI)").defineInRange("gold_current_radius", 24, 1, 256);
        DIAMOND_CURRENT_RADIUS = builder.comment("Current radius (can be changed in GUI)").defineInRange("diamond_current_radius", 40, 1, 256);
        NETHERITE_CURRENT_RADIUS = builder.comment("Current radius (can be changed in GUI)").defineInRange("netherite_current_radius", 64, 1, 256);

        builder.pop();
        SPEC = builder.build();
    }

    public static class OreFilterHolder {
        private static CompoundTag currentFilter = new CompoundTag();
        public static CompoundTag getFilter() { return currentFilter; }
        public static void setFilter(CompoundTag filter) { currentFilter = filter; }
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            configInstance = event.getConfig();
        }
    }

    public static void setCopperRadius(int value) {
        COPPER_CURRENT_RADIUS.set(value);
    }

    public static void setIronRadius(int value) {
        IRON_CURRENT_RADIUS.set(value);
    }

    public static void setGoldRadius(int value) {
        GOLD_CURRENT_RADIUS.set(value);
    }

    public static void setDiamondRadius(int value) {
        DIAMOND_CURRENT_RADIUS.set(value);
    }

    public static void setNetheriteRadius(int value) {
        NETHERITE_CURRENT_RADIUS.set(value);
    }


}