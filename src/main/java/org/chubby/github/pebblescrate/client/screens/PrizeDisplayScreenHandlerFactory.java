package org.chubby.github.pebblescrate.client.screens;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;
import org.chubby.github.pebblescrate.util.LoreUtil;
import org.chubby.github.pebblescrate.util.MessageUtils;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrizeDisplayScreenHandlerFactory implements MenuProvider {
    private final Component title;
    private final CrateConfigManager.CrateConfig crateConfig;

    public PrizeDisplayScreenHandlerFactory(Component title, CrateConfigManager.CrateConfig crateConfig) {
        this.title = title;
        this.crateConfig = crateConfig;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        final int[] currentPage = {0};

        List<CrateConfigManager.Prize> crateItems = crateConfig.prizes();
        return new ChestMenu(MenuType.GENERIC_9x6, syncId, inv, new CrateInventory(crateItems, currentPage[0]), 6) {
            @Override
            public ItemStack quickMoveStack(Player playerIn, int index) {
                return ItemStack.EMPTY; // Prevent shift-clicking items
            }

            @Override
            public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
                if (slotId == 45) { // Previous page arrow
                    if (currentPage[0] > 0) {
                        currentPage[0]--;
                        ((CrateInventory) this.getContainer()).populateInventory(crateItems, currentPage[0]);
                    }
                } else if (slotId == 53) { // Next page arrow
                    if (currentPage[0] < (crateItems.size() - 1) / 45) {
                        currentPage[0]++;
                        ((CrateInventory) this.getContainer()).populateInventory(crateItems, currentPage[0]);
                    }
                } else {
                    return; // Do nothing if clicking elsewhere
                }
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return title;
    }
}

class CrateInventory extends SimpleContainer {
    public CrateInventory(List<CrateConfigManager.Prize> crateItems, int currentPage) {
        super(54);
        populateInventory(crateItems, currentPage);
    }

    public void populateInventory(List<CrateConfigManager.Prize> crateItems, int currentPage) {
        clearContent();
        int itemsPerPage = 45;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, crateItems.size());

        double totalWeight = crateItems.stream().mapToDouble(CrateConfigManager.Prize::chance).sum();
        for (int index = startIndex; index < endIndex; index++) {
            CrateConfigManager.Prize prize = crateItems.get(index);
            ItemStack itemStack = new ItemStack(
                    ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(prize.material())),
                    prize.amount()
            );
            Component parsedName = new MessageUtils.ParseableName(prize.name()).returnMessageAsStyledText();

            double chance = prize.chance() / totalWeight * 100;
            String roundedChance = String.format("%.2f", chance);

            if (prize.nbt() != null) {
                try{
                    CompoundTag nbt = NbtUtils.snbtToStructure(prize.nbt());
                    itemStack.setTag(nbt);
                }
                catch (CommandSyntaxException e){}
            }

            if (prize.lore() != null) {
                List<String> lore = prize.lore();
                List<Component> parsedPrizeLore = lore.stream()
                        .map(line -> {
                            String message = line.replace("{chance}", roundedChance)
                                    .replace("{prize_name}", prize.name());
                            return new MessageUtils.ParseableMessage(message, null,"placeholder").returnMessageAsStyledText();
                        })
                        .collect(Collectors.toList());
                LoreUtil.setLore(itemStack, parsedPrizeLore);
            } else {
                LoreUtil.setLore(itemStack, List.of(Component.literal("Chance: " + roundedChance + "%")));
            }

            itemStack.setHoverName(parsedName);
            setItem(index - startIndex, itemStack);
        }

        // Fill the bottom row with gray stained glass
        for (int i = 45; i <= 53; i++) {
            setItem(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
        }

        Component pageText = Component.literal("Page " + (currentPage + 1) + " of " + (((crateItems.size() - 1) / 45) + 1));

        if (crateItems.size() > 45) {
            // Set the page text
            ItemStack paperStack = new ItemStack(Items.PAPER);
            paperStack.setHoverName(pageText);
            setItem(52, paperStack);

            // Set the navigation arrows
            ItemStack prevArrow = new ItemStack(Items.ARROW);
            prevArrow.setHoverName(Component.literal("Previous"));
            setItem(45, prevArrow);

            ItemStack nextArrow = new ItemStack(Items.ARROW);
            nextArrow.setHoverName(Component.literal("Next"));
            setItem(53, nextArrow);
        }
    }
}