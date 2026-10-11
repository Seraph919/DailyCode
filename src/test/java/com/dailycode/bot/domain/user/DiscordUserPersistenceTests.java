package com.dailycode.bot.domain.user;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
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
class DiscordUserPersistenceTests {

    @ParameterizedTest
    @MethodSource("invalidUsers")
    void rejectsInvalidFields(DiscordUser user, String field, @Autowired Validator validator) {
        assertThat(validator.validate(user))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly(field);
    }

    static Stream<Arguments> invalidUsers() {
        return Stream.of(
                Arguments.of(new DiscordUser(null, "alice"), "discordUserId"),
                Arguments.of(new DiscordUser("", "alice"), "discordUserId"),
                Arguments.of(new DiscordUser(" \t", "alice"), "discordUserId"),
                Arguments.of(new DiscordUser("1".repeat(21), "alice"), "discordUserId"),
                Arguments.of(new DiscordUser("123", null), "discordUsername"),
                Arguments.of(new DiscordUser("123", ""), "discordUsername"),
                Arguments.of(new DiscordUser("123", " \t"), "discordUsername"),
                Arguments.of(new DiscordUser("123", "a".repeat(101)), "discordUsername")
        );
    }

    @Test
    void acceptsBoundaryLengths(@Autowired Validator validator) {
        assertThat(validator.validate(new DiscordUser("1", "a"))).isEmpty();
        assertThat(validator.validate(new DiscordUser("1".repeat(20), "a".repeat(100)))).isEmpty();
    }

    @Test
    void jpaRejectsInvalidInsert(@Autowired EntityManager entityManager) {
        DiscordUser user = new DiscordUser("123456789012345680", " ");

        assertThatThrownBy(() -> {
            entityManager.persist(user);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void jpaRejectsInvalidUpdate(@Autowired EntityManager entityManager) {
        DiscordUser user = new DiscordUser("123456789012345681", "valid-name");
        entityManager.persist(user);
        entityManager.flush();

        user.changeUsername(" ");

        assertThatThrownBy(entityManager::flush).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void savesAndReloadsUser(@Autowired EntityManager entityManager) {
        DiscordUser user = new DiscordUser("123456789012345678", "task5-user");
        assertThat(user.getId()).isNull();

        entityManager.persist(user);
        entityManager.flush();
        Long id = user.getId();
        assertThat(id).isNotNull();
        entityManager.clear();

        DiscordUser loaded = entityManager.find(DiscordUser.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded).isNotSameAs(user);
        assertThat(loaded.getDiscordUserId()).isEqualTo("123456789012345678");
        assertThat(loaded.getDiscordUsername()).isEqualTo("task5-user");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isEqualTo(loaded.getCreatedAt());
    }

    @Test
    void updatesUsernameAndTimestampButPreservesIdentityAndCreationTime(@Autowired EntityManager entityManager) {
        DiscordUser user = new DiscordUser("123456789012345679", "original-name");
        entityManager.persist(user);
        entityManager.flush();
        Long id = user.getId();
        entityManager.clear();

        DiscordUser loaded = entityManager.find(DiscordUser.class, id);
        // Compare database round-tripped timestamps to avoid Java/PostgreSQL precision differences.
        Instant createdAt = loaded.getCreatedAt();
        Instant previousUpdatedAt = loaded.getUpdatedAt();

        loaded.changeUsername("new-name");
        entityManager.flush();
        entityManager.clear();

        DiscordUser updated = entityManager.find(DiscordUser.class, id);
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getDiscordUserId()).isEqualTo("123456789012345679");
        assertThat(updated.getDiscordUsername()).isEqualTo("new-name");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(previousUpdatedAt);
    }
}
