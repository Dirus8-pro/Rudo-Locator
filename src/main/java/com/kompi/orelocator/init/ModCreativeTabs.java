package com.kompi.orelocator.init;

import com.kompi.orelocator.OreLocatorMod;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final CreativeModeTab ORELOCATOR_TAB = new CreativeModeTab("orelocator.orelocator_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.NETHERITE_ORE_LOCATOR.get());
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            items.add(new ItemStack(ModItems.COPPER_ORE_LOCATOR.get()));
            items.add(new ItemStack(ModItems.IRON_ORE_LOCATOR.get()));
            items.add(new ItemStack(ModItems.GOLD_ORE_LOCATOR.get()));
            items.add(new ItemStack(ModItems.DIAMOND_ORE_LOCATOR.get()));
            items.add(new ItemStack(ModItems.NETHERITE_ORE_LOCATOR.get()));
        }
    };
}