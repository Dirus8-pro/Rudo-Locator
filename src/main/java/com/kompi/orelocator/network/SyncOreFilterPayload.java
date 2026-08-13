package com.kompi.orelocator.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.kompi.orelocator.RudoLocator;

public record SyncOreFilterPayload(CompoundTag filter) implements CustomPacketPayload {
    public static final Type<SyncOreFilterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RudoLocator.MODID, "sync_ore_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOreFilterPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.filter),
            buf -> new SyncOreFilterPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}