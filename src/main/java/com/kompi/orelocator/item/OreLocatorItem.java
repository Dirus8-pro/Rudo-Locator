package com.kompi.orelocator.item;

import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.OreHighlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.math.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class OreLocatorItem extends Item {
    private final Supplier<Integer> radiusSupplier;
    private final float[] color;

    public OreLocatorItem(Properties properties, Supplier<Integer> radiusSupplier, float r, float g, float b) {
        super(properties);
        this.radiusSupplier = radiusSupplier;
        this.color = new float[]{r, g, b};
    }

    private static final Map<Player, Long> lastFailedUse = new HashMap<>();

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            long currentTime = level.getGameTime();
            CompoundTag tag = stack.getOrCreateTag();
            long lastUse = tag.getLong("LastUseTime");
            if (currentTime - lastUse >= 600) {
                // Обычное сканирование
                tag.putLong("LastUseTime", currentTime);
                int customRadius = tag.getInt("CustomRadius");
                int actualRadius = customRadius > 0 ? customRadius : radiusSupplier.get();
                scanOres(level, player, actualRadius, stack);
            } else {
                // Кулдаун активен
                long now = System.currentTimeMillis();
                Long lastFailed = lastFailedUse.get(player);
                if (lastFailed != null && (now - lastFailed) < 500) {
                    // Двойной клик — убрать подсветку
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                            new OreHighlightPacket(Collections.emptyList(), level.getGameTime())
                    );
                    lastFailedUse.remove(player);
                } else {
                    // Первый клик — показать сообщение
                    player.displayClientMessage(Component.translatable("message.orelocator.recharge").withStyle(ChatFormatting.RED), true);
                    lastFailedUse.put(player, now);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void scanOres(Level level, Player player, int radius, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos playerPos = player.blockPosition();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        List<BlockPos> oreList = new ArrayList<>();

        spawnRadarWave(serverLevel, player, radius);

        CompoundTag playerData = player.getPersistentData();
        CompoundTag oreFilter = playerData.getCompound("OreFilter");

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (checkPos.getY() < level.getMinBuildHeight()
                            || checkPos.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    // Является ли блок рудой: по тегу ИЛИ по имени
                    // СТАЛО:
                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                    String path = (blockId != null) ? blockId.getPath().toLowerCase() : "";

                    boolean isOre = state.is(Tags.Blocks.ORES)
                            || state.is(Tags.Blocks.ORES_REDSTONE)
                            || block == Blocks.ANCIENT_DEBRIS
                            || path.endsWith("_ore")
                            || path.startsWith("ore_")
                            || path.contains("_ore_");

                    if (isOre) {
                        String shortKey = getOreTypeKey(state);
                        String filterKey = shortKey != null ? shortKey : (blockId != null ? blockId.toString() : null);

                        // Если руда отключена в фильтре – пропускаем
                        if (filterKey != null && oreFilter.contains(filterKey) && !oreFilter.getBoolean(filterKey)) {
                            continue;
                        }
                        oreList.add(checkPos.immutable());
                    }
                }
            }
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new OreHighlightPacket(oreList, level.getGameTime())
        );
    }

    private void spawnRadarWave(ServerLevel level, Player player, int radius) {
        double px = player.getX();
        double py = player.getY() + 0.5;
        double pz = player.getZ();
        for (int r = 1; r <= radius; r++) {
            int currentR = r;
            level.getServer().execute(() -> drawCube(level, px, py, pz, currentR));
        }
    }

    private void drawCube(ServerLevel level, double cx, double cy, double cz, int r) {
        for (int dx = -r; dx <= r; dx++) {
            spawnParticle(level, cx + dx, cy - r, cz - r);
            spawnParticle(level, cx + dx, cy - r, cz + r);
            spawnParticle(level, cx + dx, cy + r, cz - r);
            spawnParticle(level, cx + dx, cy + r, cz + r);
        }
        for (int dz = -r + 1; dz <= r - 1; dz++) {
            spawnParticle(level, cx - r, cy - r, cz + dz);
            spawnParticle(level, cx + r, cy - r, cz + dz);
            spawnParticle(level, cx - r, cy + r, cz + dz);
            spawnParticle(level, cx + r, cy + r, cz + dz);
        }
        for (int dy = -r; dy <= r; dy++) {
            spawnParticle(level, cx - r, cy + dy, cz - r);
            spawnParticle(level, cx + r, cy + dy, cz - r);
            spawnParticle(level, cx - r, cy + dy, cz + r);
            spawnParticle(level, cx + r, cy + dy, cz + r);
        }
    }

    private void spawnParticle(ServerLevel level, double x, double y, double z) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(color[0], color[1], color[2]), 1.0F);
        level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
    }

    private static String getOreTypeKey(BlockState state) {
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
        if (id != null) {
            return id.toString(); // Отдаем полный ID (например "create:zinc_ore")
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.orelocator.radius", radiusSupplier.get()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.orelocator.cooldown").withStyle(ChatFormatting.GRAY));

            CompoundTag oreFilter = new CompoundTag();
            if (Minecraft.getInstance().player != null) {
                oreFilter = Minecraft.getInstance().player.getPersistentData().getCompound("OreFilter");
            }
            int disabledCount = 0;
            for (String key : oreFilter.getAllKeys()) {
                if (!oreFilter.getBoolean(key)) disabledCount++;
            }
            if (disabledCount > 0) {
                tooltip.add(Component.translatable("tooltip.orelocator.disabled_count", disabledCount).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.orelocator.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        long lastUse = tag.getLong("LastUseTime");
        if (lastUse == 0) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        long currentTime = mc.level.getGameTime();
        return (currentTime - lastUse) < 600;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        long lastUse = tag.getLong("LastUseTime");
        if (lastUse == 0) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        long currentTime = mc.level.getGameTime();
        long elapsed = currentTime - lastUse;
        if (elapsed >= 600) return 0;
        return Math.round(13.0F * (1.0F - (float)elapsed / 600.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFFFF;
    }

    public int getRadius() {
        return radiusSupplier.get();
    }
}