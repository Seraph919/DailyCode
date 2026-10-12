package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.EnumSet;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("discord")
public class DiscordConfiguration {

    @Bean
    public DiscordReadyListener discordReadyListener() {
        return new DiscordReadyListener();
    }

    @Bean
    public PingCommandListener pingCommandListener(@Value("${discord.test-guild-id:}") String testGuildId) {
        return new PingCommandListener(testGuildId);
    }

    @Bean
    public PingCommandHandler pingCommandHandler() {
        return new PingCommandHandler();
    }

    @Bean
    public SlashCommandRouter slashCommandRouter(@Value("${discord.test-guild-id:}") String testGuildId,
                                                  List<SlashCommandHandler> handlers) {
        return new SlashCommandRouter(testGuildId, handlers);
    }

    @Bean(destroyMethod = "shutdown")
    public JDA jda(@Value("${discord.token:}") String token,
                   DiscordReadyListener listener, PingCommandListener pingListener, SlashCommandRouter router) {
        if (token.isBlank()) {
            throw new IllegalStateException("Set DISCORD_TOKEN before enabling the discord profile.");
        }

        return JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .setEnableShutdownHook(false) // Spring owns shutdown through the bean's destroy method.
                .addEventListeners(listener, pingListener, router)
                .build();
    }
}
