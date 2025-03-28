package org.chubby.github.pebblescrate.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MessageUtils {

    public static Component parse(String text, Object... placeholders) {
        String formattedText = String.format(text, placeholders);
        return Component.literal(formattedText);
    }

    public static Component parseMessageWithStyles(String text, String prizeName) {
        String[] parts = text.split("\\{prize_name}");
        List<Component> styledParts = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            styledParts.add(Component.literal(parts[i]));
            if (i < parts.length - 1) {
                styledParts.add(Component.literal(prizeName).withStyle(ChatFormatting.GOLD));
            }
        }

        MutableComponent finalMessage = Component.empty();
        for (Component part : styledParts) {
            finalMessage.append(part);
        }
        return finalMessage;
    }

    public static class ParseableMessage {
        private final String message;
        private final ServerPlayer player;
        private final String prizeName;

        public ParseableMessage(String message, @Nullable  ServerPlayer player, String prizeName) {
            this.message = message;
            this.player = player;
            this.prizeName = prizeName;
        }

        public void sendToAll() {
            if (player == null || player.server == null) return;
            Component component = parseMessageWithStyles(message, prizeName);
            for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
                onlinePlayer.sendSystemMessage(component);
            }
        }

        public void send() {
            if (player == null) return;
            Component component = parseMessageWithStyles(message, prizeName);
            player.sendSystemMessage(component);
        }

        public Component returnMessageAsStyledText() {
            return parseMessageWithStyles(message, prizeName);
        }
    }

    public static class ParseableName {
        private final String name;

        public ParseableName(String name) {
            this.name = name;
        }

        public Component returnMessageAsStyledText() {
            return parseMessageWithStyles(name, "");
        }
    }
}
