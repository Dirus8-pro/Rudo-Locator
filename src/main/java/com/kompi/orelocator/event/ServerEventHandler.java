package com.kompi.orelocator.event;

import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.SyncOreFilterPacket;
import com.kompi.orelocator.xray.OreFilterStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
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
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncOreFilterPacket(filter));
        }
    }
}