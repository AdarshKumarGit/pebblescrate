package org.chubby.github.pebblescrate.common.lootcrates;

import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BlacklistConfigManager {
    private final Path blacklistPath = Paths.get("config/pebbles-crate/blacklist.txt");

    public BlacklistConfigManager() {
        createBlacklistFile();
    }

    private void createBlacklistFile() {
        try {
            if (Files.notExists(blacklistPath)) {
                Files.createDirectories(blacklistPath.getParent());
                Files.createFile(blacklistPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<BlockPos> getBlacklist() {
        try {
            return Files.readAllLines(blacklistPath).stream()
                    .map(line -> {
                        try {
                            String[] parts = line.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            int z = Integer.parseInt(parts[2].trim());
                            return new BlockPos(x, y, z);
                        } catch (Exception e) {
                            System.err.println("Error parsing line '" + line + "': " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(pos -> pos != null)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public void addToBlacklist(BlockPos pos) {
        Set<BlockPos> blacklist = new HashSet<>(getBlacklist());
        if (!blacklist.contains(pos)) {
            blacklist.add(pos);
            saveBlacklist(blacklist);
        }
    }

    public void removeFromBlacklist(BlockPos pos) {
        Set<BlockPos> blacklist = new HashSet<>(getBlacklist());
        if (blacklist.remove(pos)) {
            saveBlacklist(blacklist);
        }
    }

    private void saveBlacklist(Set<BlockPos> blacklist) {
        try {
            Files.write(blacklistPath,
                    blacklist.stream()
                            .map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ())
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
