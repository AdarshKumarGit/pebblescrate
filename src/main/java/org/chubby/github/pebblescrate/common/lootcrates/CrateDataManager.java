package org.chubby.github.pebblescrate.common.lootcrates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CrateDataManager {
    private static final String CRATE_DATA_FILE = Paths.get("config", "pebbles_crate", "crate_data.json").toString();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TypeToken<Map<String, String>> CRATE_DATA_TYPE = new TypeToken<Map<String, String>>() {};

    public Map<BlockPos, String> loadCrateData() {
        Map<BlockPos, String> crateData = new HashMap<>();

        try (FileReader reader = new FileReader(CRATE_DATA_FILE)) {
            Map<String, String> rawCrateData = GSON.fromJson(reader, CRATE_DATA_TYPE.getType());
            if (rawCrateData != null) {
                for (Map.Entry<String, String> entry : rawCrateData.entrySet()) {
                    long posLong = Long.parseLong(entry.getKey());
                    BlockPos pos = BlockPos.of(posLong);
                    crateData.put(pos, entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return crateData;
    }

    public void saveCrateData(Map<BlockPos, String> crateData) {
        Map<String, String> rawCrateData = new HashMap<>();

        for (Map.Entry<BlockPos, String> entry : crateData.entrySet()) {
            rawCrateData.put(String.valueOf(entry.getKey().asLong()), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(CRATE_DATA_FILE)) {
            GSON.toJson(rawCrateData, CRATE_DATA_TYPE.getType(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
