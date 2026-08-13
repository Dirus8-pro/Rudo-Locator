package com.kompi.orelocator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.kompi.orelocator.RudoLocator;

import java.util.ArrayList;
import java.util.List;

public record OreHighlightPayload(List<BlockPos> orePositions, long gameTime) implements CustomPacketPayload {
    public static final Type<OreHighlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RudoLocator.MODID, "ore_highlight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreHighlightPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.gameTime);
                buf.writeVarInt(payload.orePositions.size());
                for (BlockPos pos : payload.orePositions) {
                    buf.writeBlockPos(pos);
                }
            },
            buf -> {
                long time = buf.readLong();
                int size = buf.readVarInt();
                List<BlockPos> positions = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    positions.add(buf.readBlockPos());
                }
                return new OreHighlightPayload(positions, time);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}