package com.kompi.orelocator.network;

import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class SyncOreFilterPacket {
    private final CompoundTag filter;

    public SyncOreFilterPacket(CompoundTag filter) {
        this.filter = filter;
    }

    public CompoundTag getFilter() { return filter; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(filter);
    }

    public static SyncOreFilterPacket decode(FriendlyByteBuf buf) {
        return new SyncOreFilterPacket(buf.readNbt());
    }

    public static void handleServer(SyncOreFilterPacket msg, MinecraftServer server, ServerPlayer player) {
        OreFilterStorage.saveFilter(msg.filter);
        // Рассылаем всем игрокам, передавая буфер с данными
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        msg.encode(buf);
        for (ServerPlayer p : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(p, ModNetwork.SYNC_ORE_FILTER_PACKET, buf);
        }
    }
}