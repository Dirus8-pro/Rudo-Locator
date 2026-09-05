package com.kompi.orelocator.xray;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class OreRegistryManager {
    public static final List<Block> ALL_FOUND_ORES = new ArrayList<>();

    public static void initializeOres() {
        ALL_FOUND_ORES.clear();
        TagKey<Block> commonOresTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
        System.out.println("=== OreRegistryManager: список всех руд с тегом c:ores ===");

        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().is(commonOresTag)) {
                ALL_FOUND_ORES.add(block);
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                System.out.println(id); // выводим полный идентификатор, например "create:zinc_ore"
            }
        }

        System.out.println("=============================================================");
        System.out.println("[OreLocator] Всего найдено руд: " + ALL_FOUND_ORES.size());
    }
}