package org.chubby.github.pebblescrate.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LoreUtil
{
    public static void setLore(ItemStack stack, List<Component> lore) {
        CompoundTag displayTag = stack.getOrCreateTagElement("display");
        ListTag loreList = new ListTag();

        for (Component line : lore) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }

        displayTag.put("Lore", loreList);
    }
}
