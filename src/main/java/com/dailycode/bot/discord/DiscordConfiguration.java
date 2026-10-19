package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeService;
import com.dailycode.bot.application.challenge.ChallengeJoinService;
import com.dailycode.bot.application.challenge.ChallengeTodayService;
import com.dailycode.bot.application.user.DiscordUserService;
import com.dailycode.bot.application.leetcode.LeetCodeLinkService;
import com.dailycode.bot.application.leetcode.LeetCodeVerificationService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    public LeetCodeCommandListener leetCodeCommandListener(@Value("${discord.test-guild-id:}") String testGuildId) {
        return new LeetCodeCommandListener(testGuildId);
    }

    @Bean
    public ChallengeCommandListener challengeCommandListener(@Value("${discord.test-guild-id:}") String testGuildId) {
        return new ChallengeCommandListener(testGuildId);
    }

    @Bean
    public ThreadPoolTaskExecutor discordChallengeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("discord-challenge-");
        return executor;
    }

    @Bean
    public ChallengeCommandHandler challengeCommandHandler(DiscordUserService users, ChallengeService challenges,
            ChallengeJoinService joins, ChallengeTodayService today,
            @Qualifier("discordChallengeExecutor") TaskExecutor executor) {
        return new ChallengeCommandHandler(users, challenges, joins, today, executor);
    }

    @Bean
    public ThreadPoolTaskExecutor discordLeetCodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("discord-leetcode-");
        return executor;
    }

    @Bean
    public LeetCodeCommandHandler leetCodeCommandHandler(DiscordUserService users, LeetCodeLinkService links,
            LeetCodeVerificationService verification,
            @Qualifier("discordLeetCodeExecutor") TaskExecutor executor) {
        return new LeetCodeCommandHandler(users, links, verification, executor);
    }

    @Bean
    public SlashCommandRouter slashCommandRouter(@Value("${discord.test-guild-id:}") String testGuildId,
                                                  List<SlashCommandHandler> handlers) {
        return new SlashCommandRouter(testGuildId, handlers);
    }

    @Bean
    public ThreadPoolTaskExecutor discordRegistrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("discord-registration-");
        // Spring initializes and shuts down this worker. The queue is not durable storage.
        return executor;
    }

    @Bean
    public DiscordUserRegistrationListener discordUserRegistrationListener(
            @Value("${discord.test-guild-id:}") String testGuildId,
            DiscordUserService userService,
            @Qualifier("discordRegistrationExecutor") TaskExecutor executor) {
        return new DiscordUserRegistrationListener(testGuildId, userService, executor);
    }

    @Bean(destroyMethod = "shutdown")
    public JDA jda(@Value("${discord.token:}") String token,
                   DiscordReadyListener listener, PingCommandListener pingListener, SlashCommandRouter router,
                   DiscordUserRegistrationListener registrationListener, LeetCodeCommandListener leetCodeListener,
                   ChallengeCommandListener challengeListener) {
        if (token.isBlank()) {
            throw new IllegalStateException("Set DISCORD_TOKEN before enabling the discord profile.");
        }

        return JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .setEnableShutdownHook(false) // Spring owns shutdown through the bean's destroy method.
                .addEventListeners(listener, pingListener, router, registrationListener, leetCodeListener, challengeListener)
                .build();
    }
}
