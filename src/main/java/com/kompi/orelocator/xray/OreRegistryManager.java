package com.kompi.orelocator.xray;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class OreRegistryManager {
    public static final List<Block> ALL_FOUND_ORES = new ArrayList<>();

    public static void initializeOres() {
        ALL_FOUND_ORES.clear();
        TagKey<Block> forgeOresTag = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("forge", "ores"));
        System.out.println("=== OreRegistryManager: список всех руд с тегом forge:ores ===");
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block.defaultBlockState().is(forgeOresTag)) {
                ALL_FOUND_ORES.add(block);
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                System.out.println(id); // выводим полный идентификатор, например "create:zinc_ore"
            }
        }
        System.out.println("=============================================================");
        System.out.println("[OreLocator] Всего найдено руд: " + ALL_FOUND_ORES.size());
    }
}