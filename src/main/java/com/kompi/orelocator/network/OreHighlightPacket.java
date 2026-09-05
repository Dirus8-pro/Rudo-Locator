package com.kompi.orelocator.network;

import com.kompi.orelocator.OreLocatorMod;
import com.kompi.orelocator.event.HighlightedOreStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record OreHighlightPacket(List<BlockPos> orePositions, long gameTime) implements CustomPacketPayload {

    public static final Type<OreHighlightPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OreLocatorMod.MODID, "ore_highlight"));

    public static final StreamCodec<FriendlyByteBuf, OreHighlightPacket> STREAM_CODEC = StreamCodec.of(
            OreHighlightPacket::encode,
            OreHighlightPacket::decode
    );

    private static void encode(FriendlyByteBuf buf, OreHighlightPacket msg) {
        buf.writeLong(msg.gameTime);
        buf.writeVarInt(msg.orePositions.size());
        for (BlockPos pos : msg.orePositions) {
            buf.writeBlockPos(pos);
        }
    }

    private static OreHighlightPacket decode(FriendlyByteBuf buf) {
        long time = buf.readLong();
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new OreHighlightPacket(positions, time);
    }

    public static void handle(OreHighlightPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            HighlightedOreStorage.setOres(msg.orePositions(), msg.gameTime());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}