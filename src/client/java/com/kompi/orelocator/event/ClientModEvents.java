package com.kompi.orelocator.event;

import com.kompi.orelocator.client.OreFilterHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientModEvents {

    // Храним позицию и игровой тик, когда блок был подсвечен
    private static final Map<BlockPos, Long> highlightedOres = new ConcurrentHashMap<>();

    public static void setHighlightedOres(List<BlockPos> positions) {
        highlightedOres.clear();
        Minecraft mc = Minecraft.getInstance();
        long currentTime = (mc.level != null) ? mc.level.getGameTime() : 0;

        for (BlockPos pos : positions) {
            highlightedOres.put(pos, currentTime);
        }
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null || highlightedOres.isEmpty()) return;

            long currentTime = level.getGameTime();

            // ТАЙМЕР: Удаляем блоки, если прошло > 600 тиков (30 сек) или если блок сломали (стал воздухом)
            highlightedOres.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                long spawnTime = entry.getValue();
                return (currentTime - spawnTime > 600) || level.isEmptyBlock(pos);
            });

            if (highlightedOres.isEmpty()) return;

            PoseStack poseStack = context.matrixStack();
            poseStack.pushPose();

            Vec3 cam = context.camera().getPosition();
            poseStack.translate(-cam.x, -cam.y, -cam.z);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            Matrix4f matrix = poseStack.last().pose();

            for (BlockPos pos : highlightedOres.keySet()) {
                // Определяем уникальный цвет для конкретной руды
                float[] color = getOreColor(level, pos);
                AABB box = new AABB(pos);

                renderBox(bufferBuilder, matrix, box, color[0], color[1], color[2], color[3]);
            }

            BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.endOrDiscardIfEmpty();
            if (renderedBuffer != null) {
                BufferUploader.drawWithShader(renderedBuffer);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            poseStack.popPose();
        });
    }

    private static float[] getOreColor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Проверяем персональный цвет из фильтра (если он задан)
        CompoundTag oreFilter = OreFilterHolder.getFilter();
        String filterKey = getOreTypeKeyForClient(state);

        if (filterKey != null && oreFilter.contains(filterKey + "_rgb_color")) {
            int color = oreFilter.getInt(filterKey + "_rgb_color");
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            return new float[]{r, g, b, 0.9f};
        }

        // Стандартные цвета ванильных руд
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return new float[]{0.1f, 0.1f, 0.1f, 0.8f};
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return new float[]{0.9f, 0.7f, 0.5f, 0.8f};
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return new float[]{1.0f, 0.5f, 0.2f, 0.8f};
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return new float[]{1.0f, 1.0f, 0.0f, 0.8f};
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return new float[]{1.0f, 0.0f, 0.0f, 0.8f};
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return new float[]{0.0f, 0.2f, 1.0f, 0.8f};
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return new float[]{0.0f, 1.0f, 1.0f, 0.8f};
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return new float[]{0.0f, 1.0f, 0.0f, 0.8f};
        if (block == Blocks.ANCIENT_DEBRIS) return new float[]{0.8f, 0.0f, 1.0f, 0.8f};
        if (block == Blocks.NETHER_QUARTZ_ORE) return new float[]{0.9f, 0.9f, 0.9f, 0.8f};
        if (block == Blocks.NETHER_GOLD_ORE) return new float[]{1.0f, 0.8f, 0.0f, 0.8f};

        // Модовые руды – ярко-розовый по умолчанию
        return new float[]{1.0f, 0.0f, 0.6f, 0.9f};
    }

    private static String getOreTypeKeyForClient(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return "iron";
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return "copper";
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return "gold";
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return "coal";
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return "redstone";
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return "lapis";
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return "diamond";
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return "emerald";
        if (block == Blocks.ANCIENT_DEBRIS) return "netherite";
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null ? id.toString() : null;
    }

    private static void renderBox(BufferBuilder buffer, Matrix4f matrix, AABB box, float r, float g, float b, float a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}