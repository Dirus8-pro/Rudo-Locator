package com.kompi.orelocator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class OreLocatorConfig {

    // Сохраняем прямо в папку config с именем как у Forge
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("orelocator-common.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Имя категории как в Forge
    @SerializedName("ore_locator_radius")
    public OreLocatorRadius oreLocatorRadius = new OreLocatorRadius();

    public static class OreLocatorRadius {
        // Максимальные радиусы (имена полей 1 в 1 как в Forge)
        @SerializedName("copper_max_radius")
        public int copperMaxRadius = 8;

        @SerializedName("iron_max_radius")
        public int ironMaxRadius = 12;

        @SerializedName("gold_max_radius")
        public int goldMaxRadius = 24;

        @SerializedName("diamond_max_radius")
        public int diamondMaxRadius = 40;

        @SerializedName("netherite_max_radius")
        public int netheriteMaxRadius = 64;

        // Текущие радиусы (имена полей 1 в 1 как в Forge)
        @SerializedName("copper_current_radius")
        public int copperCurrentRadius = 8;

        @SerializedName("iron_current_radius")
        public int ironCurrentRadius = 12;

        @SerializedName("gold_current_radius")
        public int goldCurrentRadius = 24;

        @SerializedName("diamond_current_radius")
        public int diamondCurrentRadius = 40;

        @SerializedName("netherite_current_radius")
        public int netheriteCurrentRadius = 64;
    }

    private static OreLocatorConfig instance;

    public static OreLocatorConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void save() {
        if (instance == null) return;
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static OreLocatorConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, OreLocatorConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OreLocatorConfig config = new OreLocatorConfig();
        config.save();
        return config;
    }

    // Вспомогательные сеттеры (как в твоём Forge-классе)
    public static void setCopperRadius(int value) {
        getInstance().oreLocatorRadius.copperCurrentRadius = value;
        save();
    }

    public static void setIronRadius(int value) {
        getInstance().oreLocatorRadius.ironCurrentRadius = value;
        save();
    }

    public static void setGoldRadius(int value) {
        getInstance().oreLocatorRadius.goldCurrentRadius = value;
        save();
    }

    public static void setDiamondRadius(int value) {
        getInstance().oreLocatorRadius.diamondCurrentRadius = value;
        save();
    }

    public static void setNetheriteRadius(int value) {
        getInstance().oreLocatorRadius.netheriteCurrentRadius = value;
        save();
    }
}