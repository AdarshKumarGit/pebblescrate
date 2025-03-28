package org.chubby.github.pebblescrate.client.screens.admin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

public class IndividualRewardEditingScreenHandler extends ChestMenu {

    private final String crateName;
    private final ItemStack previewItem;
    private final BigDecimal weight;
    private final SimpleContainer container;

    public IndividualRewardEditingScreenHandler(int syncId, Player player, String crateName, ItemStack previewItem, BigDecimal weight) {
        super(MenuType.GENERIC_9x1, syncId, player.getInventory(),new SimpleContainer(9),1);
        this.crateName = crateName;
        this.previewItem = previewItem.copy();
        this.weight = weight;
        this.container = new SimpleContainer(9);

        // Add slots for the reward items or command rewards
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(container, i, 8 + i * 18, 18));
        }

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(player.getInventory(), col, 8 + col * 18, 142));
        }
    }


    // Getters
    public String getCrateName() {
        return crateName;
    }

    public ItemStack getPreviewItem() {
        return previewItem;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public SimpleContainer getContainer() {
        return container;
    }
}