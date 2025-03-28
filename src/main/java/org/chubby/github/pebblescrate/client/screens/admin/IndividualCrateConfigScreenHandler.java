package org.chubby.github.pebblescrate.client.screens.admin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.util.List;
import java.util.stream.Collectors;

public class IndividualCrateConfigScreenHandler extends ChestMenu {

    private final Player player;
    private final String crateName;
    private final CrateConfigManager crateConfigManager;
    private final SimpleContainer container;

    public IndividualCrateConfigScreenHandler(int syncId, Player player, String crateName) {
        super(MenuType.GENERIC_9x6, syncId,player.getInventory(),new SimpleContainer(9 * 6),6);
        this.player = player;
        this.crateName = crateName;
        this.crateConfigManager = new CrateConfigManager();
        this.container = new SimpleContainer(9 * 6);

        // Add slots for the container (9x6)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + 36));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(player.getInventory(), col, 8 + col * 18, 161 + 36));
        }

        // Initialize the screen with crate items
        List<CrateConfigManager.CrateConfig> existingCrates = crateConfigManager.loadCrateConfigs();
        CrateConfigManager.CrateConfig currentCrateConfig = existingCrates.stream()
                .filter(config -> config.crateName().equals(crateName))
                .findFirst()
                .orElse(null);

        if (currentCrateConfig != null) {
            List<CrateConfigManager.Prize> crateItems = currentCrateConfig.prizes();

            for (int i = 0; i < container.getContainerSize(); i++) {
                container.setItem(i, ItemStack.EMPTY);
            }

            for (int index = 0; index < crateItems.size(); index++) {
                if (index >= container.getContainerSize()) {
                    break;
                }

                CrateConfigManager.Prize prize = crateItems.get(index);
                ResourceLocation materialIdentifier = ResourceLocation.tryParse(prize.material());

                if (materialIdentifier != null) {
                    if (ForgeRegistries.ITEMS.getValue(materialIdentifier) != Items.AIR) {
                        ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(materialIdentifier), prize.amount());
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("PebblesCrateNBT", prize.nbt() != null ? prize.nbt() : "");
                        itemStack.setTag(nbt);

                        List<Component> loreTexts = prize.commands().stream()
                                .map(Component::literal)
                                .collect(Collectors.toList());
                        setLore(itemStack, loreTexts);

                        container.setItem(index, itemStack);
                    }
                }
            }
        }
    }

    private void setLore(ItemStack itemStack, List<Component> lore) {
        CompoundTag itemNbt = itemStack.getOrCreateTagElement("display");
        ListTag loreNbt = new ListTag();

        for (Component line : lore) {
            loreNbt.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }

        itemNbt.put("Lore", loreNbt);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}