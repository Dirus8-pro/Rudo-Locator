package com.kompi.orelocator.network;

import com.kompi.orelocator.OreLocatorMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = OreLocatorMod.MODID)
public class ModNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Отправка S2C (Server -> Client)
        registrar.playToClient(
                OreHighlightPacket.TYPE,
                OreHighlightPacket.STREAM_CODEC,
                OreHighlightPacket::handle
        );

        // Двунаправленная отправка C2S / S2C
        registrar.playBidirectional(
                SyncOreFilterPacket.TYPE,
                SyncOreFilterPacket.STREAM_CODEC,
                SyncOreFilterPacket::handle
        );
    }
}