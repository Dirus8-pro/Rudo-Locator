package com.kompi.orelocator.event;

import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightedOreStorage {
    private static final Set<BlockPos> highlightedOres = ConcurrentHashMap.newKeySet();

    public static void setHighlightedOres(List<BlockPos> positions) {
        highlightedOres.clear();
        highlightedOres.addAll(positions);
        // Больше НИКАКОГО клиентского кода здесь!
    }

    public static boolean isHighlighted(BlockPos pos) {
        return highlightedOres.contains(pos);
    }
}