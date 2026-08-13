package com.kompi.orelocator.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.kompi.orelocator.RudoLocator;

public class ModNetwork {
    public static void init() {
        // Регистрируем пакеты на серверной стороне
        PayloadTypeRegistry.playS2C().register(OreHighlightPayload.TYPE, OreHighlightPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncOreFilterPayload.TYPE, SyncOreFilterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SyncOreFilterPayload.TYPE, SyncOreFilterPayload.CODEC);

        // Принимаем SyncOreFilterPayload от клиента
        ServerPlayNetworking.registerGlobalReceiver(SyncOreFilterPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                // Сохраняем фильтр в файл и рассылаем всем
                com.kompi.orelocator.xray.OreFilterStorage.saveFilter(payload.filter());
                for (var player : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, new SyncOreFilterPayload(payload.filter()));
                }
            });
        });
    }
}