package org.chubby.github.pebblescrate.common.lootcrates;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.chubby.github.pebblescrate.Pebblescrate;
import org.chubby.github.pebblescrate.common.lootcrates.CrateDataManager;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Pebblescrate.MOD_ID)
public class CrateTransformationHandler {

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos clickedPos = event.getPos();
        ItemStack heldItem = event.getItemStack();

        // Check if the held item is a crate transformer
        if (!heldItem.hasTag() || !heldItem.getTag().contains("CrateName")) return;

        String crateName = heldItem.getTag().getString("CrateName");
        CrateConfigManager.CrateConfig crateConfig = CrateConfigManager.getCrateConfig(crateName);

        if (crateConfig == null) {
            player.sendSystemMessage(Component.literal("Invalid crate configuration!"));
            return;
        }

        // Check if the clicked block is a chest or ender chest
        BlockState clickedBlockState = world.getBlockState(clickedPos);
        if (!(clickedBlockState.getBlock() instanceof AbstractChestBlock<?>)) {
            player.sendSystemMessage(Component.literal("You can only transform chests or ender chests!"));
            return;
        }


        // Save the crate data
        CrateDataManager crateDataManager = new CrateDataManager();
        Map<BlockPos, String> savedCrateData = crateDataManager.loadCrateData();
        savedCrateData.put(clickedPos, crateName);
        crateDataManager.saveCrateData(savedCrateData);

        // Consume the transformer item
        heldItem.shrink(1);

        // Notify the player
        player.sendSystemMessage(Component.literal("Transformed block into a " + crateName + " crate!"));

        // Optional: You might want to add visual or sound effects here
        world.playSound(null, clickedPos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 1.0f);

        // Prevent further interaction
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}