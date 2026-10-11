package com.dailycode.bot.application.user;

import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class DiscordUserServiceTests {

    @Test
    void registersANewUser(
            @Autowired DiscordUserService service,
            @Autowired DiscordUserRepository repository,
            @Autowired EntityManager entityManager) {
        DiscordUser registered = service.register("123456789012345700", "alice");
        entityManager.flush();
        entityManager.clear();

        assertThat(registered.getId()).isNotNull();
        assertThat(repository.findByDiscordUserId("123456789012345700"))
                .hasValueSatisfying(loaded -> {
                    assertThat(loaded.getId()).isEqualTo(registered.getId());
                    assertThat(loaded.getDiscordUsername()).isEqualTo("alice");
                });
    }

    @Test
    void repeatedRegistrationKeepsTheSameUserAndTimestamps(
            @Autowired DiscordUserService service,
            @Autowired EntityManager entityManager) {
        DiscordUser first = service.register("123456789012345701", "alice");
        entityManager.flush();
        entityManager.clear();
        DiscordUser stored = entityManager.find(DiscordUser.class, first.getId());
        Instant createdAt = stored.getCreatedAt();
        Instant updatedAt = stored.getUpdatedAt();

        DiscordUser repeated = service.register("123456789012345701", "alice");
        entityManager.flush();
        entityManager.clear();

        assertThat(repeated.getId()).isEqualTo(first.getId());
        DiscordUser reloaded = entityManager.find(DiscordUser.class, first.getId());
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void refreshesAnExistingUsername(
            @Autowired DiscordUserService service,
            @Autowired EntityManager entityManager) {
        DiscordUser first = service.register("123456789012345702", "old-name");
        entityManager.flush();
        entityManager.clear();
        DiscordUser stored = entityManager.find(DiscordUser.class, first.getId());
        Instant createdAt = stored.getCreatedAt();
        Instant previousUpdatedAt = stored.getUpdatedAt();

        DiscordUser changed = service.register("123456789012345702", "new-name");
        entityManager.flush();
        entityManager.clear();

        assertThat(changed.getId()).isEqualTo(first.getId());
        DiscordUser reloaded = entityManager.find(DiscordUser.class, first.getId());
        assertThat(reloaded.getDiscordUsername()).isEqualTo("new-name");
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void rejectsAnInvalidRenameBeforeChangingTheManagedUser(
            @Autowired DiscordUserService service,
            @Autowired EntityManager entityManager) {
        DiscordUser existing = service.register("123456789012345703", "original-name");
        entityManager.flush();

        assertThatThrownBy(() -> service.register("123456789012345703", " "))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(existing.getDiscordUsername()).isEqualTo("original-name");
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectsInvalidInputs(String discordUserId, String username, @Autowired DiscordUserService service) {
        assertThatThrownBy(() -> service.register(discordUserId, username))
                .isInstanceOf(ConstraintViolationException.class);
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(null, "alice"),
                Arguments.of("", "alice"),
                Arguments.of(" \t", "alice"),
                Arguments.of("1".repeat(21), "alice"),
                Arguments.of("123456789012345704", null),
                Arguments.of("123456789012345704", ""),
                Arguments.of("123456789012345704", " \t"),
                Arguments.of("123456789012345704", "a".repeat(101))
        );
    }
}
