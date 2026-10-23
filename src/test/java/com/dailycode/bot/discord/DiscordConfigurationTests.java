package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeService;
import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.application.challenge.ChallengeJoinService;
import com.dailycode.bot.application.challenge.ChallengeTodayService;
import com.dailycode.bot.application.challenge.DailyCompletionService;
import com.dailycode.bot.application.challenge.AutomaticCheckTargets;
import com.dailycode.bot.application.user.DiscordUserService;
import com.dailycode.bot.application.leetcode.LeetCodeLinkService;
import com.dailycode.bot.application.leetcode.LeetCodeVerificationService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.EnumSet;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiscordConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(DiscordConfiguration.class, TestServices.class);

    @Test
    void staysDisabledWithoutTheDiscordProfileEvenWithAToken() {
        try (var builders = mockStatic(JDABuilder.class)) {
            contextRunner.withPropertyValues("discord.token=test-token").run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(JDA.class);
                assertThat(context).doesNotHaveBean(DiscordReadyListener.class);
                assertThat(context).doesNotHaveBean(PingCommandListener.class);
                assertThat(context).doesNotHaveBean(SlashCommandRouter.class);
                assertThat(context).doesNotHaveBean(PingCommandHandler.class);
                assertThat(context).doesNotHaveBean(DiscordUserRegistrationListener.class);
                assertThat(context).doesNotHaveBean("discordRegistrationExecutor");
            });
            builders.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankTokensWithoutAttemptingLogin(String token) {
        try (var builders = mockStatic(JDABuilder.class)) {
            contextRunner.withPropertyValues("spring.profiles.active=discord", "discord.token=" + token)
                    .run(context -> assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("Set DISCORD_TOKEN before enabling the discord profile."));
            builders.verifyNoInteractions();
        }
    }

    @Test
    void rejectsAMissingTokenWithoutAttemptingLogin() {
        try (var builders = mockStatic(JDABuilder.class)) {
            contextRunner.withPropertyValues("spring.profiles.active=discord")
                    .run(context -> assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Set DISCORD_TOKEN before enabling the discord profile."));
            builders.verifyNoInteractions();
        }
    }

    @Test
    void registersTheListenerAndShutsDownJdaWhenSpringCloses() {
        JDA jda = mock(JDA.class);
        JDABuilder builder = mock(JDABuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(jda);
        AtomicReference<ThreadPoolTaskExecutor> registrationExecutor = new AtomicReference<>();

        try (var builders = mockStatic(JDABuilder.class)) {
            builders.when(() -> JDABuilder.createLight("test-token", EnumSet.noneOf(GatewayIntent.class)))
                    .thenReturn(builder);

            contextRunner.withPropertyValues("spring.profiles.active=discord", "discord.token=test-token")
                    .run(context -> {
                        assertThat(context).hasNotFailed().hasSingleBean(JDA.class);
                        assertThat(context.getBean(JDA.class)).isSameAs(jda);
                        verify(builder).addEventListeners(context.getBean(DiscordReadyListener.class),
                                context.getBean(PingCommandListener.class), context.getBean(SlashCommandRouter.class),
                                context.getBean(DiscordUserRegistrationListener.class),
                                context.getBean(LeetCodeCommandListener.class),
                                context.getBean(ChallengeCommandListener.class));
                        ThreadPoolTaskExecutor executor = context.getBean("discordRegistrationExecutor",
                                ThreadPoolTaskExecutor.class);
                        registrationExecutor.set(executor);
                        assertThat(executor.getCorePoolSize()).isEqualTo(1);
                        assertThat(executor.getMaxPoolSize()).isEqualTo(1);
                        assertThat(executor.getQueueCapacity()).isEqualTo(25);
                        verify(builder).setEnableShutdownHook(false);
                        verify(jda, never()).shutdown();
                    });

            // ApplicationContextRunner closes Spring's context after the callback.
            verify(jda).shutdown();
            assertThat(registrationExecutor.get().getThreadPoolExecutor().isShutdown()).isTrue();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestServices {
        @Bean
        com.dailycode.bot.application.challenge.ChallengeLeaderboardService challengeLeaderboardService() {
            return mock(com.dailycode.bot.application.challenge.ChallengeLeaderboardService.class);
        }

        @Bean
        com.dailycode.bot.application.problem.ProblemCatalogService problemCatalogService() {
            return mock(com.dailycode.bot.application.problem.ProblemCatalogService.class);
        }

        @Bean
        AutomaticCheckTargets automaticCheckTargets() {
            return mock(AutomaticCheckTargets.class);
        }
        @Bean
        com.dailycode.bot.application.challenge.ChallengeProgressService challengeProgressService() {
            return mock(com.dailycode.bot.application.challenge.ChallengeProgressService.class);
        }

        @Bean
        DailyCompletionService dailyCompletionService() {
            return mock(DailyCompletionService.class);
        }

        @Bean
        ChallengeAutomationService challengeAutomationService() {
            return mock(ChallengeAutomationService.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        ChallengeTodayService challengeTodayService() {
            return mock(ChallengeTodayService.class);
        }


        @Bean
        ChallengeJoinService challengeJoinService() {
            return mock(ChallengeJoinService.class);
        }

        @Bean
        ChallengeService challengeService() {
            return mock(ChallengeService.class);
        }

        @Bean
        LeetCodeVerificationService leetCodeVerificationService() {
            return mock(LeetCodeVerificationService.class);
        }

        @Bean
        LeetCodeLinkService leetCodeLinkService() {
            return mock(LeetCodeLinkService.class);
        }

        @Bean
        DiscordUserService discordUserService() {
            return mock(DiscordUserService.class);
        }
    }
}
