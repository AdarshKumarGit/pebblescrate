package org.chubby.github.pebblescrate.client.screens.admin;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.util.List;

public class CrateScreenHandlerFactory implements MenuProvider {
    private final CrateConfigManager.CrateConfig crateConfig;

    public CrateScreenHandlerFactory(CrateConfigManager.CrateConfig crateConfig) {
        this.crateConfig = crateConfig;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        // Create and return the screen handler for the crate interface
        return new CrateScreenHandler(syncId, inv, crateConfig);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(crateConfig.crateName());
    }
}

