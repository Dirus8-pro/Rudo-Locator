package com.kompi.orelocator.init;

import com.kompi.orelocator.OreLocatorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OreLocatorMod.MODID);

    public static final RegistryObject<CreativeModeTab> ORELOCATOR_TAB = CREATIVE_TABS.register("orelocator_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.orelocator.orelocator_tab"))
                    .icon(() -> new ItemStack(ModItems.NETHERITE_ORE_LOCATOR.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.COPPER_ORE_LOCATOR.get());
                        output.accept(ModItems.IRON_ORE_LOCATOR.get());
                        output.accept(ModItems.GOLD_ORE_LOCATOR.get());
                        output.accept(ModItems.DIAMOND_ORE_LOCATOR.get());
                        output.accept(ModItems.NETHERITE_ORE_LOCATOR.get());
                    })
                    .build());
}