package com.kompi.orelocator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;

public class OreHighlightPacket {
    private final List<BlockPos> orePositions;
    private final long gameTime;

    public OreHighlightPacket(List<BlockPos> orePositions, long gameTime) {
        this.orePositions = orePositions;
        this.gameTime = gameTime;
    }

    // Методы get для обработчика
    public List<BlockPos> getOrePositions() { return orePositions; }
    public long getGameTime() { return gameTime; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(gameTime);
        buf.writeVarInt(orePositions.size());
        for (BlockPos pos : orePositions) {
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
}