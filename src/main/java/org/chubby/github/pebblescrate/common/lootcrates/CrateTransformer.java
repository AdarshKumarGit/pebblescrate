package org.chubby.github.pebblescrate.common.lootcrates;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
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
    public ItemStack crateKeyItemStack;
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
        if (crateConfig == null || crateConfig.crateKey() == null) {
            player.sendSystemMessage(Component.literal("Error: Invalid crate configuration for " + crateName).withStyle(ChatFormatting.RED));
            return;
        }

        try {
            // Get the item from the material string
            ResourceLocation materialIdentifier = ResourceLocation.parse(crateConfig.crateKey().material());
            crateKeyItemStack = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(materialIdentifier)), amount);

            if (crateKeyItemStack.isEmpty()) {
                player.sendSystemMessage(Component.literal("Error: Invalid key item for " + crateName).withStyle(ChatFormatting.RED));
                return;
            }

            // Set the key name
            String keyName = crateConfig.crateKey().name();
            crateKeyItemStack.setHoverName(Component.literal(keyName).withStyle(ChatFormatting.GOLD));

            // Create NBT data - SIMPLIFIED APPROACH
            CompoundTag nbt = crateKeyItemStack.getOrCreateTag();
            nbt.putString("CrateName", crateConfig.crateName());

            // Apply custom NBT if provided
            if (crateConfig.crateKey().nbt() != null && !crateConfig.crateKey().nbt().isEmpty()) {
                try {
                    CompoundTag customNBT = TagParser.parseTag(crateConfig.crateKey().nbt());
                    // Merge the custom NBT with our existing NBT
                    for (String key : customNBT.getAllKeys()) {
                        nbt.put(key, Objects.requireNonNull(customNBT.get(key)));
                    }
                } catch (CommandSyntaxException e) {
                    player.sendSystemMessage(Component.literal("Warning: Invalid NBT data for key").withStyle(ChatFormatting.YELLOW));
                }
            }

            // Set lore using Minecraft's standard format
            if (crateConfig.crateKey().lore() != null && !crateConfig.crateKey().lore().isEmpty()) {
                CompoundTag display = crateKeyItemStack.getOrCreateTagElement("display");
                ListTag loreTag = new ListTag();

                for (String line : crateConfig.crateKey().lore()) {
                    Component textComponent = Component.literal(line).withStyle(ChatFormatting.GRAY);
                    loreTag.add(StringTag.valueOf(Component.Serializer.toJson(textComponent)));
                }

                display.put("Lore", loreTag);
            }

            // Add to player inventory
            player.getInventory().add(crateKeyItemStack);

            // Success message
            String message = "You received " + amount + " " + crateConfig.crateKey().name() + " for " + crateConfig.crateName() + "!";
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));

        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error creating crate key: " + e.getMessage()).withStyle(ChatFormatting.RED));
            e.printStackTrace();
        }
    }

    private void setLore(ItemStack itemStack, List<Component> lore) {
        CompoundTag display = itemStack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();

        for (int i = 0; i < lore.size(); i++) {
            Component component = lore.get(i);
            String json = Component.Serializer.toJson(component);
            loreTag.add(StringTag.valueOf(json));
        }

        display.put("Lore", loreTag);
    }
}
