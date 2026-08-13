package com.kompi.orelocator.init;

import com.kompi.orelocator.RudoLocator;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final CreativeModeTab ORELOCATOR_TAB = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.rudo-locator.rudo-locator_tab"))
            .icon(() -> new ItemStack(ModItems.NETHERITE_ORE_LOCATOR))
            .displayItems((params, output) -> {
                output.accept(ModItems.COPPER_ORE_LOCATOR);
                output.accept(ModItems.IRON_ORE_LOCATOR);
                output.accept(ModItems.GOLD_ORE_LOCATOR);
                output.accept(ModItems.DIAMOND_ORE_LOCATOR);
                output.accept(ModItems.NETHERITE_ORE_LOCATOR);
            })
            .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                RudoLocator.id("orelocator_tab"), ORELOCATOR_TAB);
    }
}