package com.kompi.orelocator.event;

import com.kompi.orelocator.client.OreFilterHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientModEvents {

    private static final Map<BlockPos, Long> highlightedOres = new ConcurrentHashMap<>();
    private static final Map<Block, float[]> colorCache = new HashMap<>();

    // Теги Forge для детекции модовых руд
    private static final TagKey<Block> FORGE_ORES = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("forge", "ores"));
    private static final TagKey<Block> FORGE_ORES_IN_GROUND = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("forge", "ore_in_ground"));

    public static void setHighlightedOres(List<BlockPos> positions, long gameTime) {
        highlightedOres.clear();
        for (BlockPos pos : positions) {
            highlightedOres.put(pos, gameTime);
        }
        HighlightedOreStorage.setHighlightedOres(positions);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || highlightedOres.isEmpty()) return;

        long currentTime = level.getGameTime();
        highlightedOres.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            long spawnTime = entry.getValue();
            return (currentTime - spawnTime > 600) || level.isEmptyBlock(pos);
        });

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.5f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (BlockPos pos : highlightedOres.keySet()) {
            float[] color = getOreColor(level, pos);
            AABB box = new AABB(pos);
            LevelRenderer.renderLineBox(poseStack, buffer, box, color[0], color[1], color[2], 1.0f);
        }
        tesselator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        poseStack.popPose();
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
            // Ванильные руды
            if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return new float[]{0.2f, 0.2f, 0.2f, 1.0f};
            if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return new float[]{0.9f, 0.7f, 0.5f, 1.0f};
            if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return new float[]{1.0f, 0.5f, 0.2f, 1.0f};
            if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE) return new float[]{1.0f, 1.0f, 0.0f, 1.0f};
            if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return new float[]{1.0f, 0.0f, 0.0f, 1.0f};
            if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return new float[]{0.0f, 0.3f, 1.0f, 1.0f};
            if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return new float[]{0.0f, 1.0f, 1.0f, 1.0f};
            if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return new float[]{0.0f, 1.0f, 0.0f, 1.0f};
            if (b == Blocks.ANCIENT_DEBRIS) return new float[]{0.8f, 0.0f, 1.0f, 1.0f};
            if (b == Blocks.NETHER_QUARTZ_ORE) return new float[]{0.9f, 0.9f, 0.9f, 1.0f};
            if (b == Blocks.NETHER_GOLD_ORE) return new float[]{1.0f, 0.8f, 0.0f, 1.0f};

            // Для модовых руд (например, Amber Ore) генерируем стабильный уникальный цвет по хэшу ID
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
            if (id != null) {
                int hash = id.toString().hashCode();
                float r = Math.abs((hash & 0xFF0000) >> 16) / 255.0f;
                float g = Math.abs((hash & 0x00FF00) >> 8) / 255.0f;
                float bCol = Math.abs(hash & 0x0000FF) / 255.0f;
                return new float[]{Math.max(r, 0.3f), Math.max(g, 0.3f), Math.max(bCol, 0.3f), 1.0f};
            }

            return new float[]{1.0f, 0.0f, 0.6f, 1.0f};
        });
    }

    private static String getOreTypeKeyForClient(BlockState state) {
        Block block = state.getBlock();

        // Стандартные ванильные ключи
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return "iron";
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return "copper";
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return "gold";
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return "coal";
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return "redstone";
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return "lapis";
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return "diamond";
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return "emerald";
        if (block == Blocks.ANCIENT_DEBRIS) return "netherite";
        if (block == Blocks.NETHER_GOLD_ORE) return "nether_gold";
        if (block == Blocks.NETHER_QUARTZ_ORE) return "quartz";

        // Детекция модовых руд
        if (isOreBlock(state)) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            return id != null ? id.toString() : null;
        }

        return null;
    }

    // Комплексный фильтр детекции любых модовых руд
    public static boolean isOreBlock(BlockState state) {
        Block block = state.getBlock();

        // 1. Проверка по тегам Forge (#forge:ores, #forge:ore_in_ground и любые теги со словом "ore")
        Holder<Block> holder = ForgeRegistries.BLOCKS.getHolder(block).orElse(null);
        if (holder != null) {
            if (holder.is(FORGE_ORES) || holder.is(FORGE_ORES_IN_GROUND)) {
                return true;
            }
            if (holder.tags().anyMatch(tag -> tag.location().getPath().contains("ore"))) {
                return true;
            }
        }

        // 2. Проверка по Registry ID (например: crystalcraft_unlimited_java:deepslate_amber_ore)
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null) {
            String path = id.getPath().toLowerCase();
            if (path.contains("ore") || path.contains("debris") || path.contains("raw_")) {
                return true;
            }
        }

        // 3. Проверка по имени локализации
        String desc = block.getDescriptionId().toLowerCase();
        return desc.contains("ore") || desc.contains("debris");
    }
}