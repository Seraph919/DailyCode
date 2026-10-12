package com.dailycode.bot.discord;

import com.dailycode.bot.support.PostgresTestConfiguration;
import com.dailycode.bot.application.user.DiscordUserService;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import jakarta.persistence.EntityManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class DiscordUserRegistrationPersistenceTests {

    @Test
    void repeatedDiscordEventsKeepOneDatabaseRowAndRefreshItsUsername(
            @Autowired DiscordUserService service,
            @Autowired DiscordUserRepository repository,
            @Autowired EntityManager entityManager) {
        String userId = "123456789012345710";
        String guildId = "1260299157571239936";
        // Only in this test: run inline to keep writes inside the rolled-back test transaction.
        DiscordUserRegistrationListener listener =
                new DiscordUserRegistrationListener(guildId, service, new SyncTaskExecutor());
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        User user = mock(User.class);
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn(guildId);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("alice");
        long rowsBefore = repository.count();

        listener.onSlashCommandInteraction(event);
        entityManager.flush();
        entityManager.clear();
        DiscordUser first = repository.findByDiscordUserId(userId).orElseThrow();

        when(user.getName()).thenReturn("alice-renamed");
        listener.onSlashCommandInteraction(event);
        entityManager.flush();
        entityManager.clear();

        DiscordUser updated = repository.findByDiscordUserId(userId).orElseThrow();
        assertThat(updated.getId()).isEqualTo(first.getId());
        assertThat(updated.getDiscordUsername()).isEqualTo("alice-renamed");
        assertThat(repository.count()).isEqualTo(rowsBefore + 1);
    }
}
