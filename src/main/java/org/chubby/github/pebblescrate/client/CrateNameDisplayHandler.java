package org.chubby.github.pebblescrate.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.chubby.github.pebblescrate.Pebblescrate;
import org.chubby.github.pebblescrate.common.lootcrates.CrateDataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Pebblescrate.MOD_ID)
public class CrateNameDisplayHandler {
    private static final Map<BlockPos, UUID> crateDisplays = new HashMap<>();
    private static final CrateDataManager crateDataManager = new CrateDataManager();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            // Only update every 20 ticks (1 second)
            if (tickCounter % 20 == 0) {
                for (ServerLevel world : event.getServer().getAllLevels()) {
                    updateCrateDisplays(world);
                }
            }
        }
    }

    private static void updateCrateDisplays(ServerLevel world) {
        Map<BlockPos, String> savedCrateData = crateDataManager.loadCrateData();

        // Remove displays for crates that no longer exist
        crateDisplays.entrySet().removeIf(entry -> {
            if (!savedCrateData.containsKey(entry.getKey())) {
                ArmorStand stand = findArmorStand(world, entry.getValue());
                if (stand != null) {
                    stand.kill();
                }
                return true;
            }
            return false;
        });

        // Update or create displays for all crates
        for (Map.Entry<BlockPos, String> entry : savedCrateData.entrySet()) {
            BlockPos pos = entry.getKey();
            String crateName = entry.getValue();

            // Check if there are nearby players (optimization)
            boolean playersNearby = !world.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(pos).inflate(32)).isEmpty();

            if (playersNearby) {
                if (!crateDisplays.containsKey(pos)) {
                    // Create new display
                    createCrateDisplay(world, pos, crateName);
                } else {
                    // Update existing display
                    ArmorStand stand = findArmorStand(world, crateDisplays.get(pos));
                    if (stand == null) {
                        createCrateDisplay(world, pos, crateName);
                    }
                }
            }
        }
    }

    private static void createCrateDisplay(ServerLevel world, BlockPos pos, String crateName) {
        // Create an invisible armor stand with a custom name
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, world);
        stand.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCustomName(Component.literal(crateName));
        stand.setCustomNameVisible(true);

        // Add to world and store UUID
        world.addFreshEntity(stand);
        crateDisplays.put(pos, stand.getUUID());
    }

    private static ArmorStand findArmorStand(Level world, UUID uuid) {
        // Since we know the block position for each armor stand, we can search in a small area around it
        BlockPos pos = null;
        for (Map.Entry<BlockPos, UUID> entry : crateDisplays.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                pos = entry.getKey();
                break;
            }
        }

        if (pos != null) {
            // Search in a small area around the known position
            return world.getEntitiesOfClass(ArmorStand.class, new AABB(pos).inflate(2.0))
                    .stream()
                    .filter(entity -> entity.getUUID().equals(uuid))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}

