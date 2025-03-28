package org.chubby.github.pebblescrate;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.chubby.github.pebblescrate.client.particles.CrateParticles;
import org.chubby.github.pebblescrate.client.screens.PrizeDisplayScreenHandlerFactory;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;
import org.chubby.github.pebblescrate.common.lootcrates.CrateDataManager;

import org.chubby.github.pebblescrate.common.lootcrates.CrateEventHandler;
import org.chubby.github.pebblescrate.core.commands.CrateCommand;
import org.chubby.github.pebblescrate.util.Task;
import org.chubby.github.pebblescrate.util.TickHandler;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(Pebblescrate.MOD_ID)
public class Pebblescrate {
    public static final String MOD_ID = "pebbles_crate";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<BlockPos> cratesInUse = Collections.synchronizedSet(new HashSet<>());
    private static final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    public static final Map<Long, List<Task>> tasks = new ConcurrentHashMap<>();

    public Pebblescrate() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        CrateConfigManager configManager = new CrateConfigManager();
        configManager.createCratesFolder();
        // Register Forge event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new TickHandler());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing Pebbles Loot Crates for Forge 1.20.1!");
        new CrateConfigManager().createCratesFolder();
    }

    private void registerCommands(final RegisterCommandsEvent event)
    {
        CrateCommand.register(event.getDispatcher());
    }

    // Handle Right-Click on a Block (for opening crates)
    @Mod.EventBusSubscriber
    public static class EventHandlers {

        @SubscribeEvent
        public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
            Level world = event.getLevel();
            if (world.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;

            ServerPlayer player = (ServerPlayer) event.getEntity();
            BlockPos clickedPos = event.getPos();
            CrateDataManager crateDataManager = new CrateDataManager();
            Map<BlockPos, String> savedCrateData = crateDataManager.loadCrateData();

            // Check if the block is a crate
            if (savedCrateData.containsKey(clickedPos)) {
                String crateName = savedCrateData.get(clickedPos);
                var crateConfig = CrateConfigManager.getCrateConfig(crateName);

                if (crateConfig != null) {
                    // Create key item
                    ItemStack crateKey = new ItemStack(Items.GOLD_NUGGET);
                    CompoundTag nbt = crateKey.getOrCreateTag();
                    nbt.putString("CrateName", crateConfig.crateName());

                    // Check if the player is holding the correct key
                    ItemStack heldItem = player.getMainHandItem();
                    if (heldItem.getItem() == crateKey.getItem() &&
                            heldItem.hasTag() &&
                            heldItem.getTag().getString("CrateName").equals(crateConfig.crateName())) {

                        // Check if crate is in use
                        if (cratesInUse.contains(clickedPos)) {
                            player.displayClientMessage(Component.literal("Someone is already using this crate!"), false);
                            return;
                        }

                        // Handle crate opening
                        CrateEventHandler crateEventHandler = new CrateEventHandler(
                                world, clickedPos, player, crateConfig.prizes(), cratesInUse, playerCooldowns, crateName
                        );

                        if (crateEventHandler.canOpenCrate()) {
                            heldItem.shrink(1);
                            crateEventHandler.showPrizesAnimation(crateEventHandler.weightedRandomSelection(crateConfig.prizes()));
                            crateEventHandler.updatePlayerCooldown();
                        }

                    } else {
                        // Open prize preview GUI
                        player.openMenu(new PrizeDisplayScreenHandlerFactory(Component.literal(crateName), crateConfig));
                    }

                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }

        // Handle Block Break Event (for removing crates)
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            BlockPos pos = event.getPos();
            CrateDataManager crateDataManager = new CrateDataManager();
            Map<BlockPos, String> savedCrateData = crateDataManager.loadCrateData();

            if (savedCrateData.containsKey(pos)) {
                savedCrateData.remove(pos);
                crateDataManager.saveCrateData(savedCrateData);

                player.displayClientMessage(Component.literal("Crate removed at: " + pos), false);
            }
        }

        // Handle Server Tick Event (for particles and crate tasks)
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                for (ServerLevel world : event.getServer().getAllLevels()) {
                    spawnParticlesForAllCrates(world);
                }
                CrateParticles.updateTimers();
            }
        }

        private static void spawnParticlesForAllCrates(ServerLevel world) {
            CrateDataManager crateDataManager = new CrateDataManager();
            Map<BlockPos, String> savedCrateData = crateDataManager.loadCrateData();

            for (BlockPos pos : savedCrateData.keySet()) {
                List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(pos).inflate(16));
                for (ServerPlayer player : nearbyPlayers) {
                    CrateParticles.spawnCrossSpiralsParticles(player, pos, world);
                }
            }
        }
    }
}
