package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiscordConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(DiscordConfiguration.class);

    @Test
    void staysDisabledWithoutTheDiscordProfileEvenWithAToken() {
        try (var builders = mockStatic(JDABuilder.class)) {
            contextRunner.withPropertyValues("discord.token=test-token").run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(JDA.class);
                assertThat(context).doesNotHaveBean(DiscordReadyListener.class);
                assertThat(context).doesNotHaveBean(PingCommandListener.class);
                assertThat(context).doesNotHaveBean(SlashCommandRouter.class);
                assertThat(context).doesNotHaveBean(PingCommandHandler.class);
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

        try (var builders = mockStatic(JDABuilder.class)) {
            builders.when(() -> JDABuilder.createLight("test-token", EnumSet.noneOf(GatewayIntent.class)))
                    .thenReturn(builder);

            contextRunner.withPropertyValues("spring.profiles.active=discord", "discord.token=test-token",
                            "discord.test-guild-id=1260299157571239936")
                    .run(context -> {
                        assertThat(context).hasNotFailed().hasSingleBean(JDA.class);
                        assertThat(context.getBean(JDA.class)).isSameAs(jda);
                        verify(builder).addEventListeners(context.getBean(DiscordReadyListener.class),
                                context.getBean(PingCommandListener.class), context.getBean(SlashCommandRouter.class));
                        verify(builder).setEnableShutdownHook(false);
                        verify(jda, never()).shutdown();
                    });

            // ApplicationContextRunner closes Spring's context after the callback.
            verify(jda).shutdown();
        }
    }
}
