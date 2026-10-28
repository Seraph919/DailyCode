package com.dailycode.bot.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;

final class CardMatchers {
    private CardMatchers() {}

    static MessageCreateData cardContaining(String text) {
        return argThat(data -> data != null && data.getEmbeds().size() == 1
                && data.getAllowedMentions().isEmpty() && text(data.getEmbeds()).contains(text));
    }

    static MessageEditData editContaining(String text) {
        return argThat(data -> data != null && data.getEmbeds().size() == 1
                && data.getAllowedMentions().isEmpty() && text(data.getEmbeds()).contains(text));
    }

    static String text(List<MessageEmbed> embeds) {
        var text = new StringBuilder();
        for (var embed : embeds) {
            text.append(embed.getTitle()).append('\n').append(embed.getDescription()).append('\n');
            for (var field : embed.getFields()) text.append(field.getName()).append(": ").append(field.getValue()).append('\n');
            if (embed.getFooter() != null) text.append(embed.getFooter().getText());
        }
        return text.toString();
    }
}
