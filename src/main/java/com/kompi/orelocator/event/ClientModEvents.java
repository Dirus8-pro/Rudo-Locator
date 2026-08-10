package com.kompi.orelocator.event;

import com.kompi.orelocator.client.OreFilterHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientModEvents {

    private static Map<BlockPos, Long> highlightedOres = new HashMap<>();

    public static void setHighlightedOres(List<BlockPos> positions, long gameTime) {
        highlightedOres.clear();
        for (BlockPos pos : positions) {
            highlightedOres.put(pos, gameTime);
        }
    }

    // Кастомный RenderType для скрытых линий — полный аналог RenderType.lines(), но с GL_GREATER
    private static final RenderType XRAY_LINES_HIDDEN = RenderType.create(
            "xray_lines_hidden",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            () -> Minecraft.getInstance().gameRenderer.getRendertypeLinesShader()))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                    }, () -> {
                        RenderSystem.disableBlend();
                    }))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("greater_depth", GL11.GL_GREATER))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .createCompositeState(false)
    );

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || highlightedOres.isEmpty()) return;

        long currentTime = level.getGameTime();
        highlightedOres.entrySet().removeIf(entry -> currentTime - entry.getValue() > 600);
        highlightedOres.keySet().removeIf(pos -> {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            boolean isOre = state.is(Tags.Blocks.ORES)
                    || (id != null && (id.getPath().endsWith("_ore") || block == Blocks.ANCIENT_DEBRIS));
            return !isOre;
        });

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // === СКРЫТЫЕ ЛИНИИ (рентген) ===
        VertexConsumer hiddenConsumer = bufferSource.getBuffer(XRAY_LINES_HIDDEN);
        for (BlockPos pos : highlightedOres.keySet()) {
            float[] color = getOreColor(level, pos);
            AABB box = new AABB(pos);
            LevelRenderer.renderLineBox(poseStack, hiddenConsumer, box, color[0], color[1], color[2], 0.35f);
        }
        bufferSource.endBatch(XRAY_LINES_HIDDEN);

        // === ВИДИМЫЕ ЛИНИИ (стандартный проход) ===
        VertexConsumer visibleConsumer = bufferSource.getBuffer(RenderType.lines());
        for (BlockPos pos : highlightedOres.keySet()) {
            float[] color = getOreColor(level, pos);
            AABB box = new AABB(pos);
            LevelRenderer.renderLineBox(poseStack, visibleConsumer, box, color[0], color[1], color[2], 1.0f);
        }
        bufferSource.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    private static float[] getOreColor(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();

        // Персональный цвет из JSON-фильтра
        CompoundTag oreFilter = OreFilterHolder.getFilter();
        String filterKey = getOreTypeKeyForClient(level.getBlockState(pos));

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

        // Модовые руды
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
        if (block == Blocks.NETHER_GOLD_ORE) return "nether_gold";
        if (block == Blocks.NETHER_QUARTZ_ORE) return "quartz";
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null ? id.toString() : null;
    }
}