package com.kompi.orelocator.client;

import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;

public class OreFilterHolder {
    // Сразу подгружаем из файла при инициализации класса, а не оставляем пустым
    private static CompoundTag currentFilter = OreFilterStorage.loadFilter();

    public static CompoundTag getFilter() {
        if (currentFilter == null) {
            currentFilter = OreFilterStorage.loadFilter();
        }
        return currentFilter;
    }

    public static void setFilter(CompoundTag filter) {
        currentFilter = filter != null ? filter : new CompoundTag();
        OreFilterStorage.saveFilter(currentFilter);
    }
}