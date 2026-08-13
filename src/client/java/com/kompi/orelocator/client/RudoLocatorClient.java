package com.kompi.orelocator.client;

import com.kompi.orelocator.event.ClientModEvents;
import com.kompi.orelocator.item.OreLocatorItem;
import com.kompi.orelocator.network.ModNetwork;
import com.kompi.orelocator.network.OreHighlightPayload;
import com.kompi.orelocator.network.SyncOreFilterPayload;
import com.kompi.orelocator.xray.OreRegistryManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class RudoLocatorClient implements ClientModInitializer {

    public static final KeyMapping OPEN_ORE_FILTER_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.rudo-locator.open_ore_filter",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.rudo-locator"
    ));

    @Override
    public void onInitializeClient() {
        // Инициализация реестра руд (динамический сбор)
        OreRegistryManager.initializeOres();

        // Регистрация рендеринга подсветки (миксин BlockMixin уже всё делает)
        ClientModEvents.register();

        // Обработка клиентских тиков для открытия GUI
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            if (OPEN_ORE_FILTER_KEY.consumeClick()
                    && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof OreLocatorItem) {
                CompoundTag currentFilter = OreFilterHolder.getFilter();
                Minecraft.getInstance().setScreen(new OreFilterScreen(player.getItemInHand(InteractionHand.MAIN_HAND), currentFilter));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(OreHighlightPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientModEvents.setHighlightedOres(payload.orePositions());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncOreFilterPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                OreFilterHolder.setFilter(payload.filter());
            });
        });
    }
}