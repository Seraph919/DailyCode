package com.dailycode.bot.repository;

import com.dailycode.bot.support.PostgresTestConfiguration;
import com.dailycode.bot.domain.user.DiscordUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class DiscordUserRepositoryTests {

    @Test
    void savesAndFindsTheMatchingDiscordUser(
            @Autowired DiscordUserRepository repository,
            @Autowired EntityManager entityManager) {
        DiscordUser saved = repository.saveAndFlush(new DiscordUser("123456789012345690", "same-name"));
        repository.saveAndFlush(new DiscordUser("123456789012345691", "same-name"));
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByDiscordUserId("123456789012345690"))
                .hasValueSatisfying(loaded -> {
                    assertThat(loaded).isNotSameAs(saved);
                    assertThat(loaded.getId()).isEqualTo(saved.getId());
                    assertThat(loaded.getDiscordUserId()).isEqualTo("123456789012345690");
                    assertThat(loaded.getDiscordUsername()).isEqualTo("same-name");
                });
        assertThat(repository.findById(saved.getId()))
                .hasValueSatisfying(loaded ->
                        assertThat(loaded.getDiscordUserId()).isEqualTo("123456789012345690"));
    }

    @Test
    void returnsEmptyWhenDiscordUserIsMissing(@Autowired DiscordUserRepository repository) {
        repository.saveAndFlush(new DiscordUser("123456789012345692", "another-user"));

        assertThat(repository.findByDiscordUserId("123456789012345693")).isEmpty();
    }

    @Test
    void rejectsDuplicateDiscordIds(@Autowired DiscordUserRepository repository) {
        repository.saveAndFlush(new DiscordUser("123456789012345694", "first-user"));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new DiscordUser("123456789012345694", "duplicate-user")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_discord_users_discord_user_id");
    }
}
