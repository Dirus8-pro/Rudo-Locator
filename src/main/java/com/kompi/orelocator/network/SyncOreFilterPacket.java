package com.kompi.orelocator.network;

import com.kompi.orelocator.OreLocatorMod;
import com.kompi.orelocator.client.OreFilterHolder;
import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncOreFilterPacket(CompoundTag filter) implements CustomPacketPayload {

    public static final Type<SyncOreFilterPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OreLocatorMod.MODID, "sync_ore_filter"));

    public static final StreamCodec<FriendlyByteBuf, SyncOreFilterPacket> STREAM_CODEC = StreamCodec.of(
            SyncOreFilterPacket::encode,
            SyncOreFilterPacket::decode
    );

    private static void encode(FriendlyByteBuf buf, SyncOreFilterPacket msg) {
        buf.writeNbt(msg.filter());
    }

    private static SyncOreFilterPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncOreFilterPacket(tag != null ? tag : new CompoundTag());
    }

    public static void handle(SyncOreFilterPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Серверная сторона: сохраняем в файл и рассылаем всем
                OreFilterStorage.saveFilter(msg.filter());
                player.getPersistentData().put("OreFilter", msg.filter());
                PacketDistributor.sendToAllPlayers(new SyncOreFilterPacket(msg.filter()));
            } else {
                // Клиентская сторона: сохраняем в клиентский держатель
                OreFilterHolder.setFilter(msg.filter());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}