package com.kompi.orelocator.xray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;

public class OreFilterStorage {
    private static final Path FILE_PATH = FMLPaths.GAMEDIR.get().resolve("config/orelocator/filter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveFilter(CompoundTag filter) {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            JsonObject json = new JsonObject();
            for (String key : filter.getAllKeys()) {
                net.minecraft.nbt.Tag tag = filter.get(key);
                if (tag instanceof net.minecraft.nbt.ByteTag) {
                    json.addProperty(key, ((net.minecraft.nbt.ByteTag) tag).getAsByte() != 0);
                } else if (tag instanceof net.minecraft.nbt.IntTag) {
                    json.addProperty(key, ((net.minecraft.nbt.IntTag) tag).getAsInt());
                }
            }
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CompoundTag loadFilter() {
        CompoundTag filter = new CompoundTag();
        if (Files.exists(FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                json.entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    if (entry.getValue().isJsonPrimitive()) {
                        var primitive = entry.getValue().getAsJsonPrimitive();
                        if (primitive.isBoolean()) {
                            filter.putBoolean(key, primitive.getAsBoolean());
                        } else if (primitive.isNumber()) {
                            filter.putInt(key, primitive.getAsInt());
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filter;
    }
}