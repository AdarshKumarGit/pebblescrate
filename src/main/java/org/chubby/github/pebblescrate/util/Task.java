package org.chubby.github.pebblescrate.util;

import net.minecraft.server.level.ServerLevel;

public record Task(ServerLevel level, long tick, Runnable action) {
    // This ensures the action can be run directly
    public void run() {
        action.run();
    }
}