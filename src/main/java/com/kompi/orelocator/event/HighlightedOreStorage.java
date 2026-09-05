package com.kompi.orelocator.event;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightedOreStorage {
    private static final Map<BlockPos, Long> HIGHLIGHTED_ORES = new ConcurrentHashMap<>();
    private static final long DURATION_TICKS = 600L;

    // Флаг: нужно ли видеокарте пересобрать модель сетки
    private static volatile boolean needsRebuild = false;

    public static void setOres(List<BlockPos> positions, long currentGameTime) {
        HIGHLIGHTED_ORES.clear();
        long expireAt = currentGameTime + DURATION_TICKS;

        for (BlockPos pos : positions) {
            HIGHLIGHTED_ORES.put(pos, expireAt);
        }
        needsRebuild = true; // Даем сигнал GPU пересобрать буфер
    }

    public static Map<BlockPos, Long> getOres() {
        return HIGHLIGHTED_ORES;
    }

    public static boolean isNeedsRebuild() {
        return needsRebuild;
    }

    public static void setNeedsRebuild(boolean value) {
        needsRebuild = value;
    }

    public static void clear() {
        HIGHLIGHTED_ORES.clear();
        needsRebuild = true;
    }
}