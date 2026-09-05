package com.kompi.orelocator.event;

import com.kompi.orelocator.network.SyncOreFilterPacket;
import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncFilter(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncFilter(sp);
        }
    }

    private static void syncFilter(ServerPlayer player) {
        CompoundTag filter = OreFilterStorage.loadFilter();
        if (!filter.isEmpty()) {
            player.getPersistentData().put("OreFilter", filter);
            PacketDistributor.sendToPlayer(player, new SyncOreFilterPacket(filter));
        }
    }
}