package org.chubby.github.pebblescrate.client.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class CrateParticles {
    private static int stepX = 1;

    private static final int PARTICLES = 2;
    private static final int PARTICLES_PER_ROTATION = 20;
    private static final int RADIUS = 1;

    public static void updateTimers() {
        stepX++;
    }

    public static void spawnSpiralParticles(ServerPlayer player, BlockPos pos, ServerLevel level) {
        for (int stepY = 0; stepY < 60; stepY += (120 / PARTICLES)) {
            double dx = -(Mth.cos((float) (((double) (stepX + stepY) / PARTICLES_PER_ROTATION) * Math.PI * 2))) * RADIUS;
            double dy = stepY / (double) PARTICLES_PER_ROTATION / 2.0;
            double dz = -(Mth.sin((float) (((double) (stepX + stepY) / PARTICLES_PER_ROTATION) * Math.PI * 2))) * RADIUS;

            double x = pos.getX() + 0.5 + dx;
            double y = pos.getY() + 0.5 + dy;
            double z = pos.getZ() + 0.5 + dz;

            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    public static void spawnCrossSpiralsParticles(ServerPlayer player, BlockPos pos, ServerLevel world) {
        for (int stepY = 0; stepY < 60; stepY += (120 / PARTICLES)) {
            double dx = -(Math.cos(((stepX + stepY) / (double) PARTICLES_PER_ROTATION) * Math.PI * 2)) * RADIUS;
            double dy = stepY / (double) PARTICLES_PER_ROTATION / 2.0;
            double dz = -(Math.sin(((stepX + stepY) / (double) PARTICLES_PER_ROTATION) * Math.PI * 2)) * RADIUS;

            double x = pos.getX() + 0.5 + dx;
            double y = pos.getY() + 1.5 + dy;
            double z = pos.getZ() + 0.5 + dz;

            world.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    public static void rewardParticles(ServerPlayer player, BlockPos pos) {
        ServerLevel world = player.serverLevel();

        for (int i = 0; i < 5; i++) {
            world.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.BLOCKS, 0.5f, 0.5f);
            //world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL, SoundSource.BLOCKS, 0.5f, 1f);
        }

        double offsetX = 0.5;
        double offsetY = 0.2;
        double offsetZ = 0.5;

        world.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.getX() + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + offsetZ,
                50,
                0.0f,
                0.0f,
                0.0f,
                0.1f
        );
    }
}
