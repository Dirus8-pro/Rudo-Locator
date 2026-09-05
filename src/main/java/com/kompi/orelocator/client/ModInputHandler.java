package com.kompi.orelocator.client;

import com.kompi.orelocator.item.OreLocatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ModInputHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (ModClientSetup.OPEN_ORE_FILTER_KEY.consumeClick()
                && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof OreLocatorItem) {
            CompoundTag currentFilter = OreFilterHolder.getFilter();
            mc.setScreen(new OreFilterScreen(player.getItemInHand(InteractionHand.MAIN_HAND), currentFilter));
        }
    }
}