package com.kompi.orelocator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OreHighlightPacket {
    private final List<BlockPos> orePositions;
    private final long gameTime;

    public OreHighlightPacket(List<BlockPos> orePositions, long gameTime) {
        this.orePositions = orePositions;
        this.gameTime = gameTime;
    }

    public static void encode(OreHighlightPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.gameTime);
        buf.writeVarInt(msg.orePositions.size());
        for (BlockPos pos : msg.orePositions) {
            buf.writeBlockPos(pos);
        }
    }

    public static OreHighlightPacket decode(FriendlyByteBuf buf) {
        long time = buf.readLong();
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new OreHighlightPacket(positions, time);
    }

    public static void handle(OreHighlightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Клиентская обработка – сохраняем список в статическое поле
            com.kompi.orelocator.event.ClientModEvents.setHighlightedOres(msg.orePositions, msg.gameTime);
        });
        ctx.get().setPacketHandled(true);
    }
}