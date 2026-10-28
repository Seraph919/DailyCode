package com.dailycode.bot.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.List;

final class CommandCards {
    private CommandCards() {}

    static MessageCreateData create(String title, String description) {
        return new MessageCreateBuilder().setAllowedMentions(List.of())
                .setEmbeds(embed(title, description)).build();
    }

    static MessageEditData edit(String title, String description) {
        return new MessageEditBuilder().setReplace(true).setAllowedMentions(List.of())
                .setEmbeds(embed(title, description)).build();
    }

    private static MessageEmbed embed(String title, String description) {
        return new EmbedBuilder().setColor(0x5865F2).setTitle(title)
                .setDescription(description).setFooter("DailyCode").build();
    }
}
