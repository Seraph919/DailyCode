package com.dailycode.bot.domain.leetcode;

import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class LeetCodeAccountPersistenceTests {

    @Test
    void persistsAccountAndManagesTimestamps(@Autowired EntityManager em) {
        DiscordUser user = new DiscordUser("123456789012345681", "alice");
        em.persist(user);
        em.flush();

        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        em.persist(account);
        em.flush();

        Long id = account.getId();
        assertThat(id).isNotNull();
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
        assertThat(account.isVerified()).isFalse();

        em.clear();
        LeetCodeAccount loaded = em.find(LeetCodeAccount.class, id);
        assertThat(loaded.getLeetcodeUsername()).isEqualTo("alice_lc");
        assertThat(loaded.getDiscordUser().getDiscordUserId()).isEqualTo("123456789012345681");
    }

    @Test
    void rejectsDuplicateDiscordUser(@Autowired EntityManager em) {
        DiscordUser user = new DiscordUser("123456789012345682", "bob");
        em.persist(user);
        em.flush();

        LeetCodeAccount account1 = new LeetCodeAccount(user, "bob_lc1");
        em.persist(account1);
        em.flush();

        LeetCodeAccount account2 = new LeetCodeAccount(user, "bob_lc2");

        assertThatThrownBy(() -> em.persist(account2))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_leetcode_accounts_discord_user");
    }

    @Test
    void rejectsCaseInsensitiveDuplicateLeetCodeUsername(@Autowired EntityManager em) {
        DiscordUser user1 = new DiscordUser("123456789012345683", "user1");
        DiscordUser user2 = new DiscordUser("123456789012345684", "user2");
        em.persist(user1);
        em.persist(user2);
        em.flush();

        LeetCodeAccount account1 = new LeetCodeAccount(user1, "UniqueCoder");
        em.persist(account1);
        em.flush();

        LeetCodeAccount account2 = new LeetCodeAccount(user2, "uniquecoder");

        assertThatThrownBy(() -> em.persist(account2))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_leetcode_accounts_username_lower");
    }

    @Test
    void verificationLifecycleMethodsWorkAsExpected(@Autowired Validator validator) {
        DiscordUser user = new DiscordUser("123456789012345685", "charlie");
        LeetCodeAccount account = new LeetCodeAccount(user, "charlie_lc");
        Instant now = Instant.now();

        // Initially no active challenge
        assertThat(account.hasActiveVerificationChallenge(now)).isFalse();

        // Start verification
        account.startVerification("dailycode-token123", now.plus(10, ChronoUnit.MINUTES), now);
        assertThat(account.hasActiveVerificationChallenge(now)).isTrue();
        assertThat(account.getVerificationToken()).isEqualTo("dailycode-token123");

        // Expired check
        assertThat(account.hasActiveVerificationChallenge(now.plus(11, ChronoUnit.MINUTES))).isFalse();

        // Complete verification
        account.completeVerification("dailycode-token123", now.plus(1, ChronoUnit.MINUTES));
        assertThat(account.isVerified()).isTrue();
        assertThat(account.getVerifiedAt()).isNotNull();
        assertThat(account.getVerificationToken()).isNull();
        assertThat(account.getVerificationExpiresAt()).isNull();
        assertThat(account.hasActiveVerificationChallenge(now)).isFalse();

        // Cannot start verification once verified
        assertThatThrownBy(() -> account.startVerification("new-token", now.plus(10, ChronoUnit.MINUTES), now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesFields(@Autowired Validator validator) {
        DiscordUser user = new DiscordUser("123456789012345686", "dave");
        assertThat(validator.validate(new LeetCodeAccount(null, "dave_lc"))).isNotEmpty();
        assertThat(validator.validate(new LeetCodeAccount(user, ""))).isNotEmpty();
        assertThat(validator.validate(new LeetCodeAccount(user, "   "))).isNotEmpty();
        assertThat(validator.validate(new LeetCodeAccount(user, "a".repeat(101)))).isNotEmpty();
    }
}
