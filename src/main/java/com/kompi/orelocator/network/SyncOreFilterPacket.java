package com.kompi.orelocator.network;

import com.kompi.orelocator.client.OreFilterHolder;
import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class SyncOreFilterPacket {
    private final CompoundTag filter;

    public SyncOreFilterPacket(CompoundTag filter) {
        this.filter = filter;
    }

    public CompoundTag getFilter() {
        return filter;
    }

    public static void encode(SyncOreFilterPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.filter);
    }

    public static SyncOreFilterPacket decode(FriendlyByteBuf buf) {
        return new SyncOreFilterPacket(buf.readNbt());
    }

    public static void handle(SyncOreFilterPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Серверная сторона: сохраняем в файл и рассылаем всем
                OreFilterStorage.saveFilter(msg.filter);
                player.getPersistentData().put("OreFilter", msg.filter);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncOreFilterPacket(msg.filter));
            } else {
                // Клиентская сторона: просто сохраняем в держатель
                OreFilterHolder.setFilter(msg.filter);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}