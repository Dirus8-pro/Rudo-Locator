package com.kompi.orelocator.event;

import com.kompi.orelocator.client.OreFilterHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "orelocator", value = Dist.CLIENT)
public class ClientModEvents {

    private static final Map<Block, float[]> colorCache = new HashMap<>();

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Map<BlockPos, Long> ores = HighlightedOreStorage.getOres();

        if (level == null || ores.isEmpty()) return;

        long currentTime = level.getGameTime();

        // Удаляем истёкшие или уже выкопанные руды
        ores.entrySet().removeIf(entry ->
                currentTime > entry.getValue() ||
                        level.isEmptyBlock(entry.getKey()) ||
                        isNotOreAnymore(level.getBlockState(entry.getKey()))
        );

        if (ores.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        // Получаем матрицу камеры для правильного преобразования координат
        Matrix4f mat = poseStack.last().pose();

        // 1. Включаем сквозной X-Ray
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 2. Строим сетку через Tesselator
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos pos : ores.keySet()) {
            float[] color = getOreColor(level, pos);
            float r = color[0], g = color[1], b = color[2], a = 1.0f;

            // Координаты относительно камеры (гарантия, что куб привязан к миру)
            float minX = (float) (pos.getX() - camPos.x);
            float minY = (float) (pos.getY() - camPos.y);
            float minZ = (float) (pos.getZ() - camPos.z);

            drawBox(mat, buffer, minX, minY, minZ, r, g, b, a);
        }

        // 3. Отправляем в видеокарту
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        // 4. Восстанавливаем стейты
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // Матрица mat передается в каждую вершину: это фиксирует куб на земле при поворотах головы
    private static void drawBox(Matrix4f mat, BufferBuilder buffer, float minX, float minY, float minZ, float r, float g, float b, float a) {
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;

        // Нижний квадрат
        line(mat, buffer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(mat, buffer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(mat, buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(mat, buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Верхний квадрат
        line(mat, buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(mat, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(mat, buffer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(mat, buffer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // 4 стойки
        line(mat, buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(mat, buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(mat, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(mat, buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void line(Matrix4f mat, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
    }

    private static boolean isNotOreAnymore(BlockState state) {
        if (state.isAir()) return true;
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return true;
        String path = id.getPath();
        return !path.endsWith("_ore") && !path.endsWith("_ores") && block != Blocks.ANCIENT_DEBRIS;
    }

    private static float[] getOreColor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        CompoundTag oreFilter = OreFilterHolder.getFilter();
        String filterKey = getOreTypeKeyForClient(state);

        if (filterKey != null && oreFilter.contains(filterKey + "_rgb_color")) {
            int color = oreFilter.getInt(filterKey + "_rgb_color");
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            return new float[]{r, g, b, 1.0f};
        }

        return colorCache.computeIfAbsent(block, b -> {
            if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return new float[]{0.65f, 0.65f, 0.7f, 1.0f};
            if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return new float[]{0.9f, 0.7f, 0.5f, 1.0f};
            if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return new float[]{1.0f, 0.5f, 0.2f, 1.0f};
            if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE) return new float[]{1.0f, 1.0f, 0.0f, 1.0f};
            if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return new float[]{1.0f, 0.1f, 0.1f, 1.0f};
            if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return new float[]{0.1f, 0.4f, 1.0f, 1.0f};
            if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return new float[]{0.0f, 1.0f, 1.0f, 1.0f};
            if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return new float[]{0.0f, 1.0f, 0.0f, 1.0f};
            if (b == Blocks.ANCIENT_DEBRIS) return new float[]{0.8f, 0.0f, 1.0f, 1.0f};
            return new float[]{1.0f, 0.0f, 0.6f, 1.0f};
        });
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
}