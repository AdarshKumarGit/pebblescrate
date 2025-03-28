package org.chubby.github.pebblescrate.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FloatingPrizeItemEntity extends ItemEntity {
    private int ticksElapsed = 0;

    public FloatingPrizeItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
        setNoGravity(true);
        setPickUpDelay(Integer.MAX_VALUE);
        setInvisible(true);
        setInvulnerable(true);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;
        if (ticksElapsed >= 100) {
            this.kill();
        }
    }
}
