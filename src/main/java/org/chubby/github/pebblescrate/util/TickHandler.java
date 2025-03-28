package org.chubby.github.pebblescrate.util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.chubby.github.pebblescrate.Pebblescrate;

import java.util.List;

public class TickHandler {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Only process tasks at the END phase to avoid duplicate processing
        if (event.phase == TickEvent.Phase.END) {
            processTasks(event.getServer());
        }
    }

    private void processTasks(MinecraftServer server) {
        // Check if server has any levels
        if (server == null || !server.getAllLevels().iterator().hasNext()) {
            return;
        }

        long currentTick = server.getAllLevels().iterator().next().getGameTime();

        List<Task> tickTasks = Pebblescrate.tasks.remove(currentTick);
        if (tickTasks != null) {
            tickTasks.forEach(task -> {
                try {
                    task.action().run();
                } catch (Exception e) {
                    // Log exception but don't crash the server
                    System.err.println("Error executing task at tick " + currentTick + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}