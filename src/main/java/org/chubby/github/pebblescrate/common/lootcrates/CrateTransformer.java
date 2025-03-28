package org.chubby.github.pebblescrate.common.lootcrates;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.chubby.github.pebblescrate.util.MessageUtils.ParseableMessage;

public class CrateTransformer {
    private final String crateName;
    private final Player player;
    private final CrateConfigManager.CrateConfig crateConfig;
    private final ItemStack crateItemStack = new ItemStack(Items.PAPER);

    public CrateTransformer(String crateName, Player player) {
        this.crateName = crateName;
        this.player = player;
        this.crateConfig = CrateConfigManager.getCrateConfig(crateName);
    }

    public void giveTransformer() {
        // Create and add instructions to lore
        List<Component> instructions = List.of(
                Component.literal("Right-click a chest/ender chest to").withStyle(ChatFormatting.GOLD),
                Component.literal("transform it into a " + crateName).withStyle(ChatFormatting.GOLD)
        );

        setLore(crateItemStack, instructions);

        CompoundTag nbt = crateItemStack.getOrCreateTag();
        nbt.putString("CrateName", crateName);
        crateItemStack.setHoverName(Component.literal(crateName));

        player.sendSystemMessage(Component.literal("Giving " + crateName + " to " + player.getName().getString()));

        player.getInventory().add(crateItemStack);

        if (player instanceof ServerPlayer serverPlayer) {
            String message = "Successfully gave " + crateName + " to " + player.getName().getString();
            new ParseableMessage(message, serverPlayer, "placeholder").send();
        }
    }

    public void giveKey(int amount, Player admin) {
        if (crateConfig == null || crateConfig.crateKey() == null) return;

        ResourceLocation materialIdentifier = ResourceLocation.parse(crateConfig.crateKey().material());
        ItemStack crateKeyItemStack = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(materialIdentifier)), amount);

        if (!crateKeyItemStack.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component parsedName = new ParseableMessage(crateConfig.crateKey().name(), serverPlayer, "placeholder")
                        .returnMessageAsStyledText();
                crateKeyItemStack.setHoverName(parsedName);
            }

            if (crateConfig.crateKey().nbt() != null) {
                try{
                    CompoundTag nbt = TagParser.parseTag(crateConfig.crateKey().nbt());
                    crateKeyItemStack.setTag(nbt);
                }
                catch (CommandSyntaxException e){

                }
            }

            CompoundTag nbt = crateKeyItemStack.getOrCreateTag();
            nbt.putString("CrateName", crateConfig.crateName());

            // Set the lore
            List<Component> parsedCrateKeyLore = crateConfig.crateKey().lore().stream()
                    .map(line -> new ParseableMessage(line, (ServerPlayer) player, "placeholder").returnMessageAsStyledText())
                    .toList();
            setLore(crateKeyItemStack, parsedCrateKeyLore);

            player.getInventory().add(crateKeyItemStack);

            if (player instanceof ServerPlayer serverPlayer) {
                String message = "You received " + amount + " " + crateConfig.crateKey().name() +
                        " for " + crateConfig.crateName() + "!";
                new ParseableMessage(message, serverPlayer, "placeholder").send();
            }
        }
    }

    private void setLore(ItemStack itemStack, List<Component> lore) {
        ListTag listTag = new ListTag();
        for(int i=0;i<lore.size();i++){
            CompoundTag tag = new CompoundTag();
            tag.putString("LoreTag",lore.get(i).getString());
            listTag.add(i,tag);
        }
        itemStack.getOrCreateTagElement("display").put("Lore", listTag);
    }
}
