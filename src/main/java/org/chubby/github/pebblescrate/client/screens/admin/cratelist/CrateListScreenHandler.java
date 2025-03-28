package org.chubby.github.pebblescrate.client.screens.admin.cratelist;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.MenuProvider;
import org.chubby.github.pebblescrate.client.screens.admin.crateconfig.CrateConfigScreenHandler;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;
import org.chubby.github.pebblescrate.util.MessageUtils;


public class CrateListScreenHandler extends ChestMenu {
    private final CrateConfigManager crateConfigManager = new CrateConfigManager();
    private final Player player;

    public CrateListScreenHandler(int syncId, Player player) {
        super(MenuType.GENERIC_9x6, syncId, player.getInventory(), new SimpleContainer(9 * 6), 6);
        this.player = player;
        initializeInventory();
    }

    private void initializeInventory() {
        var existingCrates = crateConfigManager.loadCrateConfigs();
        SimpleContainer inventory = (SimpleContainer) this.getContainer();

        for (int index = 0; index < existingCrates.size(); index++) {
            var crateConfig = existingCrates.get(index);
            ItemStack crateItem = new ItemStack(Items.ENDER_CHEST);
            Component formattedName = new MessageUtils.ParseableName(crateConfig.crateName()).returnMessageAsStyledText();
            crateItem.setHoverName(formattedName);
            inventory.setItem(index, crateItem);
        }

        // Fill last row with gray stained glass pane
        for (int i = 45; i < 54; i++) {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.setHoverName(Component.empty());
            inventory.setItem(i, pane);
        }
    }

    @Override
    public void clicked(int slotIndex, int clickData, ClickType actionType, Player player) {
        if (actionType == ClickType.THROW || actionType == ClickType.CLONE ||
                actionType == ClickType.SWAP || actionType == ClickType.PICKUP_ALL) {
            return;
        }

        player.displayClientMessage(Component.literal("Slot index: " + slotIndex), false);
        player.displayClientMessage(Component.literal("Item: " + this.getContainer().getItem(slotIndex)), false);

        if (slotIndex >= 0 && slotIndex <= 44 && actionType == ClickType.PICKUP) {
            var existingCrates = crateConfigManager.loadCrateConfigs();
            if (slotIndex < existingCrates.size()) {
                var crateConfig = existingCrates.get(slotIndex);
                player.displayClientMessage(Component.literal("Opening config for crate: " + crateConfig.crateName()), false);

                // Open Crate Config Screen
                player.closeContainer();
                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal(crateConfig.crateName() + " Configuration");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player p) {
                        return new CrateConfigScreenHandler(syncId, p.getInventory(), crateConfig.crateName());
                    }
                });
            }
        }
    }
}
