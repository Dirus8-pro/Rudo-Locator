package com.kompi.orelocator.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.OreHighlightPayload;
import com.kompi.orelocator.xray.OreFilterStorage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

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

            // Новый способ чтения NBT через DataComponents
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            long lastUse = tag.getLong("LastUseTime");
            if (currentTime - lastUse >= 600) {
                tag.putLong("LastUseTime", currentTime);
                tag.putLong("LastUseWallTime", System.currentTimeMillis());
                // Сохраняем обратно в компонент
                CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.merge(tag));

                int customRadius = tag.getInt("CustomRadius");
                int actualRadius = customRadius > 0 ? customRadius : radiusSupplier.get();
                scanOres(level, player, actualRadius, stack);
            } else {
                long now = System.currentTimeMillis();
                Long lastFailed = lastFailedUse.get(player);
                if (lastFailed != null && (now - lastFailed) < 500) {
                    // Двойной клик — очистить подсветку
                    OreHighlightPayload clearPayload = new OreHighlightPayload(Collections.emptyList(), level.getGameTime());
                    ServerPlayNetworking.send((ServerPlayer) player, new OreHighlightPayload(Collections.emptyList(), level.getGameTime()));
                    lastFailedUse.remove(player);
                } else {
                    player.displayClientMessage(Component.translatable("message.rudo-locator.recharge").withStyle(ChatFormatting.RED), true);
                    lastFailedUse.put(player, now);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        // Проверяем Shift через наш скрытый от компилятора метод
        if (isShiftDownSafe()) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            int customRadius = tag.getInt("CustomRadius");
            int actualRadius = customRadius > 0 ? customRadius : radiusSupplier.get();

            // Принудительно ставим БЕЛЫЙ цвет (.withStyle(ChatFormatting.WHITE)) для радиуса
            tooltipComponents.add(Component.translatable("tooltip.rudo-locator.radius", actualRadius).withStyle(ChatFormatting.WHITE));
            tooltipComponents.add(Component.translatable("tooltip.rudo-locator.cooldown").withStyle(ChatFormatting.GRAY));
        } else {
            // Если шифт не нажат, показываем только подсказку
            tooltipComponents.add(Component.translatable("tooltip.rudo-locator.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // ГЕНИАЛЬНЫЙ ХАК: Вызываем проверку Shift через текст.
    // Gradle не видит здесь импорта клиентских классов и пропускает сборку!
    private static boolean isShiftDownSafe() {
        try {
            // Пробуем деобфусцированное имя (для IDE)
            Class<?> screenClass;
            try {
                screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
                return (Boolean) screenClass.getMethod("hasShiftDown").invoke(null);
            } catch (ClassNotFoundException e) {
                // Если не нашли — используем Intermediary имя Fabric (для релизной сборки)
                screenClass = Class.forName("net.minecraft.class_437");
                return (Boolean) screenClass.getMethod("method_25442").invoke(null);
            }
        } catch (Throwable e) {
            return false;
        }
    }

    // ==========================================
    // ПОЛОСКА ПЕРЕЗАРЯДКИ (РАБОТАЕТ БЕЗ ИМПОРТОВ)
    // ==========================================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null) return false;
        long lastUseWall = tag.getLong("LastUseWallTime");
        if (lastUseWall == 0) return false;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUseWall;
        return elapsed < 30_000; // 30 секунд
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null) return 0;
        long lastUseWall = tag.getLong("LastUseWallTime");
        if (lastUseWall == 0) return 0;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUseWall;

        if (elapsed >= 30_000) return 0;

        return Math.round(13.0F * (1.0F - (float) elapsed / 30_000.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFFFF; // Белый
    }


    // ==========================================
    // СКАНЕР, ВОЛНА, ЧАСТИЦЫ (БЕЗ ИЗМЕНЕНИЙ)
    // ==========================================

    private void scanOres(Level level, Player player, int radius, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos playerPos = player.blockPosition();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        List<BlockPos> oreList = new ArrayList<>();

        spawnRadarWave(serverLevel, player, radius);

        CompoundTag oreFilter = OreFilterStorage.loadFilter();

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

                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                    boolean isOre = state.is(ConventionalBlockTags.ORES)
                            || (blockId != null && (blockId.getPath().endsWith("_ore") || block == Blocks.ANCIENT_DEBRIS));

                    if (isOre) {
                        String shortKey = getOreTypeKey(state);
                        String filterKey = shortKey != null ? shortKey : (blockId != null ? blockId.toString() : null);

                        if (filterKey != null && oreFilter.contains(filterKey) && !oreFilter.getBoolean(filterKey)) {
                            continue;
                        }
                        oreList.add(checkPos.immutable());
                    }
                }
            }
        }

        ServerPlayNetworking.send(serverPlayer, new OreHighlightPayload(oreList, level.getGameTime()));
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
        return null;
    }

    public int getRadius() {
        return radiusSupplier.get();
    }
}