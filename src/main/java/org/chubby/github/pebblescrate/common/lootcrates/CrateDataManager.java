package org.chubby.github.pebblescrate.common.lootcrates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.chubby.github.pebblescrate.Pebblescrate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CrateDataManager implements ResourceManagerReloadListener {
    private static final Path CONFIG_DIRECTORY = Paths.get("config", "pebbles_crate", "data");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TypeToken<Map<String, String>> CRATE_DATA_TYPE = new TypeToken<Map<String, String>>() {};

    private Map<BlockPos, String> crateData = new HashMap<>();
    private static CrateDataManager INSTANCE;

    public CrateDataManager() {
        ensureConfigDirectoryExists();
        MinecraftForge.EVENT_BUS.register(this);
        loadAllCrateData();
    }

    public static CrateDataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CrateDataManager();
        }
        return INSTANCE;
    }

    private void ensureConfigDirectoryExists() {
        try {
            Files.createDirectories(CONFIG_DIRECTORY);
        } catch (IOException e) {
            Pebblescrate.LOGGER.error("Failed to create config directory: {}", CONFIG_DIRECTORY, e);
        }
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        loadAllCrateData();
    }

    public Map<BlockPos, String> loadCrateData() {
        return new HashMap<>(crateData);
    }

    private void loadAllCrateData() {
        Map<BlockPos, String> newCrateData = new HashMap<>();

        try (Stream<Path> files = Files.list(CONFIG_DIRECTORY)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(file -> {
                        try (FileReader reader = new FileReader(file.toFile())) {
                            Map<String, String> rawCrateData = GSON.fromJson(reader, CRATE_DATA_TYPE.getType());
                            if (rawCrateData != null) {
                                for (Map.Entry<String, String> entry : rawCrateData.entrySet()) {
                                    try {
                                        long posLong = Long.parseLong(entry.getKey());
                                        BlockPos pos = BlockPos.of(posLong);
                                        newCrateData.put(pos, entry.getValue());
                                    } catch (NumberFormatException e) {
                                        Pebblescrate.LOGGER.error("Invalid BlockPos in crate data file {}: {}",
                                                file.getFileName(), entry.getKey(), e);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            Pebblescrate.LOGGER.error("Error reading crate data file: {}", file.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            Pebblescrate.LOGGER.error("Error listing crate data files", e);
        }

        this.crateData = newCrateData;
        Pebblescrate.LOGGER.info("Loaded {} crate locations from config files", crateData.size());
    }

    public void saveCrateData(Map<BlockPos, String> crateData) {
        this.crateData = new HashMap<>(crateData);

        // Create a single consolidated JSON file for now
        // You could implement logic to split into multiple files if needed
        Map<String, String> rawCrateData = new HashMap<>();

        for (Map.Entry<BlockPos, String> entry : crateData.entrySet()) {
            rawCrateData.put(String.valueOf(entry.getKey().asLong()), entry.getValue());
        }

        Path mainDataFile = CONFIG_DIRECTORY.resolve("crate_data.json");
        try (FileWriter writer = new FileWriter(mainDataFile.toFile())) {
            GSON.toJson(rawCrateData, writer);
            Pebblescrate.LOGGER.info("Saved {} crate locations to {}",
                    rawCrateData.size(), mainDataFile.getFileName());
        } catch (IOException e) {
            Pebblescrate.LOGGER.error("Error saving crate data", e);
        }
    }

    // Add this to your existing map-by-dimension saving logic if needed
    public void saveCrateDataByDimension(Map<BlockPos, String> crateData) {
        this.crateData = new HashMap<>(crateData);

        // Group by dimension
        Map<String, Map<String, String>> dimensionData = new HashMap<>();

        for (Map.Entry<BlockPos, String> entry : crateData.entrySet()) {
            BlockPos pos = entry.getKey();
            String dimension = getDimensionKey(pos);

            dimensionData.computeIfAbsent(dimension, k -> new HashMap<>())
                    .put(String.valueOf(pos.asLong()), entry.getValue());
        }

        // Save each dimension to its own file
        for (Map.Entry<String, Map<String, String>> entry : dimensionData.entrySet()) {
            String dimension = entry.getKey();
            Map<String, String> dimData = entry.getValue();

            Path dimFile = CONFIG_DIRECTORY.resolve("crate_data_" + dimension + ".json");
            try (FileWriter writer = new FileWriter(dimFile.toFile())) {
                GSON.toJson(dimData, writer);
                Pebblescrate.LOGGER.info("Saved {} crate locations for dimension {} to {}",
                        dimData.size(), dimension, dimFile.getFileName());
            } catch (IOException e) {
                Pebblescrate.LOGGER.error("Error saving crate data for dimension {}", dimension, e);
            }
        }
    }

    private String getDimensionKey(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return "unknown";
        }
        return level.dimension().location().toString();
    }

    private String getDimensionKey(BlockPos pos, Level level) {
        return level.dimension().location().toString();
    }
}