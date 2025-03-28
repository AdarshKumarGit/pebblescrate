package org.chubby.github.pebblescrate.client.screens.admin.crateconfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraftforge.network.NetworkHooks;
import org.chubby.github.pebblescrate.client.screens.admin.cratelist.CrateListScreenHandler;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;
import org.chubby.github.pebblescrate.common.lootcrates.CrateTransformer;


public class CrateConfigScreenHandler extends ChestMenu {

    private static final int INVENTORY_SIZE = 9 * 3;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private final Player player;
    private final CrateConfigManager crateConfigManager = new CrateConfigManager();
    private final String crateName;

    public CrateConfigScreenHandler(int syncId, Inventory playerInventory, String crateName) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, new SimpleContainer(INVENTORY_SIZE),3);
        this.player = playerInventory.player;
        this.crateName = crateName;
        setupInventory();
        addPlayerInventorySlots(playerInventory);
    }

    private void setupInventory() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE).setHoverName(Component.literal("")));
        }

        inventory.setItem(12, new ItemStack(Items.PAPER).setHoverName(Component.literal("Get Crate").withStyle(ChatFormatting.GOLD)));
        inventory.setItem(13, new ItemStack(Items.TRIPWIRE_HOOK).setHoverName(Component.literal("Get Key").withStyle(ChatFormatting.GOLD)));
        inventory.setItem(14, new ItemStack(Items.ITEM_FRAME).setHoverName(Component.literal("Configure Prize (Web Editor)").withStyle(ChatFormatting.GOLD)));
        inventory.setItem(18, new ItemStack(Items.ARROW).setHoverName(Component.literal("Back").withStyle(ChatFormatting.RED)));
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, index, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP || clickType == ClickType.PICKUP_ALL) {
            return;
        }

        var crateTransformer = new CrateTransformer(crateName, player);

        if (slotIndex == 18) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, (MenuProvider) new CrateListScreenHandler(serverPlayer.containerMenu.containerId, serverPlayer), buf -> {});
            }
        }

        if (slotIndex == 12) {
            crateTransformer.giveTransformer();
        }

        if (slotIndex == 13) {
            crateTransformer.giveKey(1, player);
        }

        if (slotIndex == 14) {
            var url = "https://pebblescrate.sethi.tech/";
            var clickableLink = Component.literal(url).withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, url)));
            player.sendSystemMessage(Component.literal("To edit the config on the web UI, navigate to: ").withStyle(ChatFormatting.GOLD).append(clickableLink));
        }
    }
}
