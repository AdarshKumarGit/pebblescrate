package org.chubby.github.pebblescrate.client.screens.admin.cratelist;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.chubby.github.pebblescrate.common.lootcrates.BlacklistConfigManager;
import org.chubby.github.pebblescrate.common.lootcrates.CrateDataManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class ActiveCrateListScreenHandler extends ChestMenu {

    private final Player player;
    private final CrateDataManager crateDataManager = new CrateDataManager();
    private final BlacklistConfigManager blacklistManager = new BlacklistConfigManager();
    private final List<BlockPos> activeCratePositions;
    private final List<String> activeCrateNames;

    public ActiveCrateListScreenHandler(int syncId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, new SimpleContainer(9 * 6), 6);
        this.player = playerInventory.player;
        this.activeCratePositions = List.copyOf(crateDataManager.loadCrateData().keySet());
        this.activeCrateNames = List.copyOf(crateDataManager.loadCrateData().values());
        initializeInventory();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void initializeInventory() {
        Set<BlockPos> blacklist = blacklistManager.getBlacklist();
        for (int index = 0; index < activeCratePositions.size() && index < getContainer().getContainerSize(); index++) {
            BlockPos cratePos = activeCratePositions.get(index);
            String crateName = activeCrateNames.get(index);

            BlockState blockState = player.level().getBlockState(cratePos);
            Block block = blockState.getBlock();
            ItemStack crateItem = new ItemStack(block.asItem());

            crateItem.setHoverName(Component.literal(crateItem.getHoverName().getString() + " - " + crateName));

            if (!blacklist.contains(cratePos)) {
                crateItem.enchant(Enchantments.VANISHING_CURSE, 1);
            }

            getContainer().setItem(index, crateItem);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (actionType == ClickType.THROW || actionType == ClickType.CLONE ||
                actionType == ClickType.SWAP || actionType == ClickType.PICKUP_ALL) {
            return;
        }

        if (slotIndex >= activeCratePositions.size()) {
            return;
        }

        BlockPos cratePos = activeCratePositions.get(slotIndex);
        Set<BlockPos> blacklist = blacklistManager.getBlacklist();

        if (blacklist.contains(cratePos)) {
            blacklistManager.removeFromBlacklist(cratePos);
        } else {
            blacklistManager.addToBlacklist(cratePos);
        }

        player.closeContainer();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Blacklist Particles");
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return new ActiveCrateListScreenHandler(i,inventory);
            }
        });
    }
}