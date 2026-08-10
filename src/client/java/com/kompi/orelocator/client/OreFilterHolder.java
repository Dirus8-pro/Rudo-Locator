package com.kompi.orelocator.client;

import net.minecraft.nbt.CompoundTag;

public class OreFilterHolder {
    private static CompoundTag currentFilter = new CompoundTag();

    public static CompoundTag getFilter() {
        return currentFilter;
    }

    public static void setFilter(CompoundTag filter) {
        currentFilter = filter;
    }
}