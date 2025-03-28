package org.chubby.github.pebblescrate.util;

import com.google.gson.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Type;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("itemId", ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString());
        jsonObject.addProperty("amount", itemStack.getCount());

        if (itemStack.hasCustomHoverName()) {
            jsonObject.addProperty("displayName", Component.Serializer.toJson(itemStack.getHoverName()));
        }

        // Save NBT data
        if (itemStack.hasTag()) {
            jsonObject.addProperty("nbt", itemStack.getTag().toString());
        }

        if (itemStack.hasTag() && itemStack.getTag().contains("display") && itemStack.getTag().getCompound("display").contains("Lore")) {
            JsonArray loreJsonArray = new JsonArray();
            var loreNbtList = itemStack.getTag().getCompound("display").getList("Lore", 8);
            for (int i = 0; i < loreNbtList.size(); i++) {
                loreJsonArray.add(loreNbtList.getString(i));
            }
            jsonObject.add("lore", loreJsonArray);
        }

        return jsonObject;
    }

    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String itemId = jsonObject.get("itemId").getAsString();
        int amount = jsonObject.get("amount").getAsInt();
        String displayName = jsonObject.has("displayName") ? jsonObject.get("displayName").getAsString() : null;
        String nbt = jsonObject.has("nbt") ? jsonObject.get("nbt").getAsString() : null;

        return new ItemConfig(itemId, nbt, amount, displayName).toItemStack();
    }

    public static class ItemConfig {
        private final String itemId;
        private final String nbt;
        private final int amount;
        private final String displayName;

        public ItemConfig(String itemId, String nbt, int amount, String displayName) {
            this.itemId = itemId;
            this.nbt = nbt;
            this.amount = amount;
            this.displayName = displayName;
        }

        public ItemStack toItemStack() {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(itemId));
            if (item == null) {
                throw new JsonParseException("Invalid item ID: " + itemId);
            }

            ItemStack itemStack = new ItemStack(item, amount);

            // Apply NBT data if present
            if (nbt != null) {
                try {
                    CompoundTag nbtCompound = TagParser.parseTag(nbt);
                    itemStack.setTag(nbtCompound);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (displayName != null) {
                itemStack.setHoverName(Component.Serializer.fromJson(displayName));
            }

            return itemStack;
        }
    }
}
