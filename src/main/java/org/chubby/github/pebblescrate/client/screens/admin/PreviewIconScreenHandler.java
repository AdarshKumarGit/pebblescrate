package org.chubby.github.pebblescrate.client.screens.admin;



import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PreviewIconScreenHandler extends ChestMenu {

    private final ItemStack oddsSumItem = new ItemStack(Items.PAPER);
    private final CrateConfigManager crateConfigManager = new CrateConfigManager();
    private final String crateName;

    public PreviewIconScreenHandler(int syncId, Player playerInventory, String crateName) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory.getInventory(), new SimpleContainer(9*3),3);
        this.crateName = crateName;

        // Add slots for the container (9x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(this.getContainer(), col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory.getInventory(), col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory.getInventory(), col, 8 + col * 18, 142));
        }

        List<CrateConfigManager.CrateConfig> existingCrates = crateConfigManager.loadCrateConfigs();
        CrateConfigManager.CrateConfig currentCrateConfig = existingCrates.stream()
                .filter(config -> config.crateName().equals(crateName))
                .findFirst()
                .orElse(null);

        List<CrateConfigManager.Prize> crateItems = currentCrateConfig.prizes();

        for (int i = 0; i < getContainer().getContainerSize(); i++) {
            getContainer().setItem(i, ItemStack.EMPTY);
        }

        for (int index = 0; index < crateItems.size(); index++) {
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

                    getContainer().setItem(index, itemStack);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotIndex, int clickData, ClickType clickType, Player player) {
        // Open the IndividualRewardEditingScreen for the clicked item
        // Save the preview items and their weights to a JSON file
        if (slotIndex == 9 * 3 - 1) {
            // Save the preview items
            player.sendSystemMessage(Component.literal("Saving..."));

            // Get the current crate configurations
            List<CrateConfigManager.CrateConfig> currentCrateConfigs = crateConfigManager.loadCrateConfigs();

            // Update or add the crate configuration
            ArrayList<CrateConfigManager.Prize> updatedPrizes = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                ItemStack stack = getContainer().getItem(i);
                if (!stack.isEmpty()) {
                    BigDecimal weight = getWeightFromLore(stack);
                    CrateConfigManager.Prize prize = currentCrateConfigs.stream()
                            .filter(config -> config.crateName().equals(crateName))
                            .findFirst()
                            .orElse(null)
                            .prizes()
                            .get(i);

                    updatedPrizes.add(new CrateConfigManager.Prize(
                            prize.material(),
                            prize.name(),
                            prize.amount(),
                            prize.nbt(),
                            prize.commands(),
                            prize.messageToOpener(),
                            prize.broadcast(),
                            prize.lore(),
                            weight.intValue()
                    ));
                }
            }

            CrateConfigManager.CrateConfig existingCrateConfig = currentCrateConfigs.stream()
                    .filter(config -> config.crateName().equals(crateName))
                    .findFirst()
                    .orElse(null);

            if (existingCrateConfig != null) {
                for(CrateConfigManager.Prize prize : updatedPrizes){
                    existingCrateConfig.prizes().add(prize);
                }
            } else {
                CrateConfigManager.CrateConfig currentCrateConfig = currentCrateConfigs.stream()
                        .filter(config -> config.crateName().equals(crateName))
                        .findFirst()
                        .orElse(null);

                CrateConfigManager.CrateConfig newCrateConfig = new CrateConfigManager.CrateConfig(
                        crateName,
                        currentCrateConfig.crateKey(),
                        currentCrateConfig.screenName(),
                        updatedPrizes
                );
                crateConfigManager.setCrateConfig(crateName, newCrateConfig);
            }

            // Save the updated crate configurations
            crateConfigManager.saveCrateConfigs(currentCrateConfigs);

            player.sendSystemMessage(Component.literal("Saved!"));


             player.openMenu(new SimpleMenuProvider((syncId, inventory, p) ->
                    new IndividualCrateConfigScreenHandler(syncId, p, crateName), Component.literal(crateName + " Config")));

        } else {
            super.clicked(slotIndex, clickData, clickType, player);
        }
    }

    private void setLore(ItemStack itemStack, List<Component> lore) {
        CompoundTag displayTag = itemStack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();

        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }

        displayTag.put("Lore", loreTag);
    }

    private void updateOddsSumItem() {
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = getContainer().getItem(i);
            if (!stack.isEmpty()) {
                ListTag lore = stack.getTagElement("display") != null ?
                        stack.getTagElement("display").getList("Lore", Tag.TAG_STRING) : null;

                if (lore != null && lore.size() > 0) {
                    Component line = Component.Serializer.fromJson(lore.getString(0));
                    if (line != null && line.getString().contains(": ")) {
                        String weightStr = line.getString().split(": ")[1];
                        BigDecimal weight = new BigDecimal(weightStr);
                        totalWeight = totalWeight.add(weight);
                    }
                }
            }
        }

        oddsSumItem.setHoverName(Component.literal("Total Weight: " + totalWeight));
    }

    private BigDecimal getWeightFromLore(ItemStack itemStack) {
        ListTag lore = itemStack.getTagElement("display") != null ?
                itemStack.getTagElement("display").getList("Lore", Tag.TAG_STRING) : null;

        if (lore != null && lore.size() > 0) {
            Component line = Component.Serializer.fromJson(lore.getString(0));
            if (line != null && line.getString().contains(": ")) {
                return new BigDecimal(line.getString().split(": ")[1]);
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 9 * 3) {
                if (!this.moveItemStackTo(itemstack1, 9 * 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 9 * 3, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}