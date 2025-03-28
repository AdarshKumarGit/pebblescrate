package org.chubby.github.pebblescrate.client.screens.admin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.util.List;
import java.util.Objects;

public class CrateScreenHandler extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final CrateConfigManager.CrateConfig crateConfig;
    private final SimpleContainer inventory;

    public CrateScreenHandler(int syncId, Inventory playerInventory, CrateConfigManager.CrateConfig crateConfig) {
        super(null, syncId);
        this.playerInventory = playerInventory;
        this.crateConfig = crateConfig;

        final int rows = 6;
        final int columns = 9;
        this.inventory = new SimpleContainer(columns * rows);

        // Populate the inventory with the prizes and their chances
        populateInventory();

        // Add the slots for the inventory
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                addSlot(new Slot(inventory, column + row * columns, 8 + column * 18, 18 + row * 18));
            }
        }

        // Add the player's inventory slots
        final int playerInventoryStartX = 8;
        final int playerInventoryStartY = 140;
        final int playerHotbarStartY = 198;

        for (int row = 0; row <= 2; row++) {
            for (int column = 0; column <= 8; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        playerInventoryStartX + column * 18,
                        playerInventoryStartY + row * 18
                ));
            }
        }

        for (int column = 0; column <= 8; column++) {
            addSlot(new Slot(playerInventory, column, playerInventoryStartX + column * 18, playerHotbarStartY));
        }
    }

    private void populateInventory() {
        // Populate the inventory with the prizes and their chances
        List<CrateConfigManager.Prize> prizes = crateConfig.prizes();
        for (int index = 0; index < prizes.size(); index++) {
            CrateConfigManager.Prize prize = prizes.get(index);
            // Add the prize item to the inventory with its chance as lore
            ItemStack itemStack = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(prize.material()))));
            setLore(itemStack, List.of(Component.literal("Chance: " + prize.chance())));
            inventory.setItem(index, itemStack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void setLore(ItemStack itemStack, List<Component> lore) {
        CompoundTag itemNbt = itemStack.getOrCreateTagElement("display");
        ListTag loreNbt = new ListTag();
        for (Component line : lore) {
            loreNbt.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        itemNbt.put("Lore", loreNbt);
    }
}
