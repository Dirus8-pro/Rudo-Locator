package com.kompi.orelocator.network;

import com.kompi.orelocator.OreLocatorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OreLocatorMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        CHANNEL.messageBuilder(OreHighlightPacket.class, 0)
                .encoder(OreHighlightPacket::encode)
                .decoder(OreHighlightPacket::decode)
                .consumerNetworkThread(OreHighlightPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncOreFilterPacket.class, 1)
                .encoder(SyncOreFilterPacket::encode)
                .decoder(SyncOreFilterPacket::decode)
                .consumerNetworkThread(SyncOreFilterPacket::handle)
                .add();
    }
}