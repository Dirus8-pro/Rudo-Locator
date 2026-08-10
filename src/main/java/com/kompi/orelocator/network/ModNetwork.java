package com.kompi.orelocator.network;

import com.kompi.orelocator.RudoLocator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class ModNetwork {
    public static final ResourceLocation ORE_HIGHLIGHT_PACKET = RudoLocator.id("ore_highlight");
    public static final ResourceLocation SYNC_ORE_FILTER_PACKET = RudoLocator.id("sync_ore_filter");

    public static void init() {
        // Сервер принимает SyncOreFilterPacket от клиента
        ServerPlayNetworking.registerGlobalReceiver(SYNC_ORE_FILTER_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    SyncOreFilterPacket packet = SyncOreFilterPacket.decode(buf);
                    SyncOreFilterPacket.handleServer(packet, server, player);
                });
    }
}