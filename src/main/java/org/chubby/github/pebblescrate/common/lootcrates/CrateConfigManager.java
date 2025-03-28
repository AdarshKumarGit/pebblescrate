package org.chubby.github.pebblescrate.common.lootcrates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.item.ItemStack;
import org.chubby.github.pebblescrate.util.ItemStackAdapter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateConfigManager
{
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
            .create();
    private final File configDirectory = new File("config/pebbles-crate/crates");
    private static final Map<String, CrateConfig> crateConfigs = new HashMap<>();

    public CrateConfigManager() {
        createCratesFolder();
        loadCrateConfigs();
    }

    public void createCratesFolder() {
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }
    }

    public static CrateConfig getCrateConfig(String crateName) {
        return crateConfigs.get(crateName);
    }

    public void saveCrateConfigs(List<CrateConfig> updatedCrateConfigs) {
        crateConfigs.clear();
        for (CrateConfig crateConfig : updatedCrateConfigs) {
            String crateName = crateConfig.crateName();
            crateConfigs.put(crateName, crateConfig);
            File file = new File(configDirectory, crateName + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(crateConfig, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setCrateConfig(String crateName, CrateConfig crateConfig) {
        crateConfigs.put(crateName, crateConfig);
        saveCrateConfigs(new ArrayList<>(crateConfigs.values()));
    }

    public List<CrateConfig> loadCrateConfigs() {
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }

        List<CrateConfig> loadedConfigs = new ArrayList<>();
        File[] files = configDirectory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            crateConfigs.clear();
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    CrateConfig crateConfig = gson.fromJson(reader, CrateConfig.class);
                    if (crateConfig != null) {
                        String crateName = crateConfig.crateName();
                        crateConfigs.put(crateName, crateConfig);
                        loadedConfigs.add(crateConfig);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return loadedConfigs;
    }

    public record CrateConfig(
            String crateName, CrateKey crateKey, String screenName, List<Prize> prizes
    ){}

    public record CrateKey(
            String material, String name, String nbt, List<String> lore
    ){}

    public record Prize(String name,
                        String material,
                        int amount,
                        String nbt,
                        List<String> commands,
                        @Nullable String broadcast,
                        @Nullable String messageToOpener,
                        List<String> lore,
                        int chance){}
}
