package com.kompi.orelocator.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightedOreStorage {
    private static final Set<BlockPos> highlightedOres = ConcurrentHashMap.newKeySet();

    public static void setHighlightedOres(List<BlockPos> positions) {
        Set<BlockPos> oldPositions = new HashSet<>(highlightedOres);

        highlightedOres.clear();
        highlightedOres.addAll(positions);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            for (BlockPos pos : oldPositions) {
                markBlockForRenderUpdate(mc, pos);
            }
            for (BlockPos pos : positions) {
                markBlockForRenderUpdate(mc, pos);
            }
        }
    }

    public static boolean isHighlighted(BlockPos pos) {
        return highlightedOres.contains(pos);
    }

    public static void clear() {
        Set<BlockPos> oldPositions = new HashSet<>(highlightedOres);
        highlightedOres.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            for (BlockPos pos : oldPositions) {
                markBlockForRenderUpdate(mc, pos);
            }
        }
    }

    private static void markBlockForRenderUpdate(Minecraft mc, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        mc.levelRenderer.setBlocksDirty(x, y, z, x, y, z);
    }
}