package org.chubby.github.pebblescrate.client.screens.admin;

import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.util.List;


public class CrateNameScreenHandler extends ChestMenu {
    private final Player player;

    public CrateNameScreenHandler(int syncId, Player player) {
        super(MenuType.GENERIC_9x1, syncId, player.getInventory(), new SimpleContainer(9), 1);
        this.player = player;

        // Save reference to the inventory
        SimpleContainer inventory = (SimpleContainer) this.getContainer();

        CrateConfigManager crateConfigManager = new CrateConfigManager();

        // Fill the inventory with paper with modified name
        for (int i = 1; i < 9; i++) {
            ItemStack paper = new ItemStack(Items.PAPER);
            paper.setHoverName(Component.literal("Add item to empty slot to name crate"));
            inventory.setItem(i, paper);
        }

        // Load existing crates
        crateConfigManager.loadCrateConfigs();

        // Add the crate name slot
        this.addSlot(new Slot(inventory, 0, 8, 18));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        if (slotId == 0 && getContainer().getItem(0).getItem() == Items.NAME_TAG) {
            String crateName = getContainer().getItem(0).getHoverName().getString();

            // Check if the crate name is already taken
            CrateConfigManager configManager = new CrateConfigManager();
            List<CrateConfigManager.CrateConfig> existingCrates = configManager.loadCrateConfigs();

            for (CrateConfigManager.CrateConfig crateConfig : existingCrates) {
                if (crateConfig.crateName().equals(crateName)) {
                    player.sendSystemMessage(Component.literal("Crate name already taken"));
                    return;
                }
            }

            // Open the preview icon screen
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (syncId, inventory, p) -> new PreviewIconScreenHandler(syncId, p, crateName),
                        Component.literal("Loot Icon Editor")
                ));
            }
        } else {
            super.clicked(slotId, dragType, clickTypeIn, player);
        }
    }
}