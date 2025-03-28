package org.chubby.github.pebblescrate.common.lootcrates;


import com.mojang.brigadier.ParseResults;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.Pebblescrate;
import org.chubby.github.pebblescrate.client.particles.CrateParticles;
import org.chubby.github.pebblescrate.util.FloatingPrizeItemEntity;
import org.chubby.github.pebblescrate.util.MessageUtils;
import org.chubby.github.pebblescrate.util.Task;

import java.util.*;

public class CrateEventHandler {
    private static final long COOLDOWN_TIME = 8000L;
    private final Level world;
    private final BlockPos pos;
    private final ServerPlayer player;
    private final List<CrateConfigManager.Prize> prizes;
    private final Set<BlockPos> cratesInUse;
    private final Map<UUID, Long> playerCooldowns;
    private final String crateName;
    private FloatingPrizeItemEntity lastFloatingPrizeItemEntity;
    private final Object floatingPrizeItemEntityLock = new Object();
    private final Random random = new Random();

    public CrateEventHandler(Level world, BlockPos pos, ServerPlayer player, List<CrateConfigManager.Prize> prizes,
                             Set<BlockPos> cratesInUse, Map<UUID, Long> playerCooldowns, String crateName) {
        this.world = world;
        this.pos = pos;
        this.player = player;
        this.prizes = prizes;
        this.cratesInUse = cratesInUse;
        this.playerCooldowns = playerCooldowns;
        this.crateName = crateName;
    }

    public CrateConfigManager.Prize weightedRandomSelection(List<CrateConfigManager.Prize> prizes) {
        int totalWeight = prizes.stream().mapToInt(CrateConfigManager.Prize::chance).sum();
        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (CrateConfigManager.Prize prize : prizes) {
            cumulativeWeight += prize.chance();
            if (randomValue < cumulativeWeight) {
                return prize;
            }
        }
        throw new IllegalStateException("No prize could be selected.");
    }

    private void spawnFloatingItem(CrateConfigManager.Prize prize) {
        if (world instanceof ServerLevel) {
            CrateParticles.rewardParticles(player, pos);
            revealPrize(prize, true);

            if (prize.messageToOpener() != null && !prize.messageToOpener().isEmpty()) {
                String message = prize.messageToOpener().replace("{prize_name}", prize.name());
                new MessageUtils.ParseableMessage(message, player, prize.name()).send();
            }

            if (prize.broadcast() != null && !prize.broadcast().isEmpty()) {
                String broadcast = prize.broadcast()
                        .replace("{prize_name}", prize.name())
                        .replace("{player_name}", player.getDisplayName().getString())
                        .replace("{crate_name}", crateName);

                new MessageUtils.ParseableMessage(broadcast, player, prize.name()).sendToAll();
            }
        }
    }

    private void revealPrize(CrateConfigManager.Prize prize, boolean isFinalPrize) {
        removeFloatingItem();

        ResourceLocation prizeId = ResourceLocation.tryParse(prize.material());
        ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(prizeId));

        if (prize.nbt() != null && !prize.nbt().isBlank()) {
            try{
                CompoundTag nbt = TagParser.parseTag(prize.nbt());
                itemStack.setTag(nbt);
            }
            catch (Exception e){}
        }

        itemStack.setHoverName(Component.literal(prize.name()));
        double height = isFinalPrize ? pos.getY() + 1.0 : pos.getY();
        Vec3 spawnPos = new Vec3(pos.getX() + 0.5, height + 1.5, pos.getZ() + 0.5);

        FloatingPrizeItemEntity floatingPrizeItemEntity = new FloatingPrizeItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, itemStack);
        world.addFreshEntity(floatingPrizeItemEntity);

        synchronized (floatingPrizeItemEntityLock) {
            lastFloatingPrizeItemEntity = floatingPrizeItemEntity;
        }
    }

    public void showPrizesAnimation(CrateConfigManager.Prize finalPrize) {
        if (world instanceof ServerLevel) {
            long currentTime = System.currentTimeMillis();
            long lastCrateOpenTime = playerCooldowns.getOrDefault(player.getUUID(), 0L);

            if (currentTime - lastCrateOpenTime < COOLDOWN_TIME) {
                long remainingCooldown = (COOLDOWN_TIME - (currentTime - lastCrateOpenTime)) / 1000;
                player.displayClientMessage(Component.literal("You can open another crate in " + remainingCooldown + " seconds.").withStyle(ChatFormatting.RED), false);
                return;
            }

            cratesInUse.add(pos);
            int animationPrizesCount = 10;
            long delayBetweenPrizes = 6L;

            for (int i = 0; i < animationPrizesCount; i++) {
                CrateConfigManager.Prize randomPrize = weightedRandomSelection(prizes);
                addTask((ServerLevel) world, delayBetweenPrizes * i, () -> showRandomPrizeRunnable(randomPrize).run());
            }

            long finalPrizeDelay = delayBetweenPrizes * (animationPrizesCount + 1);
            addTask((ServerLevel) world, finalPrizeDelay, () -> {
                revealPrize(finalPrize, true);
                spawnFloatingItem(finalPrize);

                for (String command : finalPrize.commands()) {
                    String cmd = command.replace("{player_name}", player.getDisplayName().getString());
                    try {
                        ParseResults<CommandSourceStack> parseResults = player.getServer().getCommands().getDispatcher().parse(cmd, player.getServer().createCommandSourceStack());
                        player.getServer().getCommands().getDispatcher().execute(parseResults);
                    } catch (Exception e) {
                        player.displayClientMessage(Component.literal("Error executing command: " + command), false);
                    }
                }
            });

            long removeCrateDelay = finalPrizeDelay + 100;
            addTask((ServerLevel) world, removeCrateDelay, () -> cratesInUse.remove(pos));
        }
    }

    private Runnable showRandomPrizeRunnable(CrateConfigManager.Prize prize) {
        return () -> {
            revealPrize(prize, false);
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
        };
    }

    private void removeFloatingItem() {
        synchronized (floatingPrizeItemEntityLock) {
            if (lastFloatingPrizeItemEntity != null) {
                lastFloatingPrizeItemEntity.kill();
                lastFloatingPrizeItemEntity = null;
            }
        }
    }

    public boolean canOpenCrate() {
        long currentTime = System.currentTimeMillis();
        long lastCrateOpenTime = playerCooldowns.get(player.getUUID());

        if (currentTime - lastCrateOpenTime < COOLDOWN_TIME) {
            long remainingCooldown = (COOLDOWN_TIME - (currentTime - lastCrateOpenTime)) / 1000;
            player.displayClientMessage(
                    Component.literal("You can open another crate in $remainingCooldown seconds.").withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }
        return true;
    }

    public void updatePlayerCooldown() {
        long currentTime = System.currentTimeMillis();
        playerCooldowns.put(player.getUUID(),currentTime);
    }

    public void addTask(ServerLevel world, long tickDelay, Runnable action) {
        long taskTick = world.getGameTime() + tickDelay;
        Pebblescrate.tasks.computeIfAbsent(taskTick, k -> new ArrayList<>()).add(new Task(world, taskTick, action));
    }
}
