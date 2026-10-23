package com.dailycode.bot.repository;

import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class LeetCodeAccountRepositoryTests {

    @Autowired
    private LeetCodeAccountRepository accounts;

    @Autowired
    private EntityManager em;

    @Test
    void findsByDiscordUserId() {
        DiscordUser user = new DiscordUser("123456789012345687", "eve");
        em.persist(user);
        LeetCodeAccount account = new LeetCodeAccount(user, "eve_lc");
        em.persist(account);
        em.flush();

        Optional<LeetCodeAccount> found = accounts.findByDiscordUser_DiscordUserId("123456789012345687");
        assertThat(found).isPresent();
        assertThat(found.get().getLeetcodeUsername()).isEqualTo("eve_lc");

        assertThat(accounts.findByDiscordUser_DiscordUserId("nonexistent")).isEmpty();
    }

    @Test
    void findsByLeetCodeUsernameIgnoreCase() {
        DiscordUser user = new DiscordUser("123456789012345688", "frank");
        em.persist(user);
        LeetCodeAccount account = new LeetCodeAccount(user, "FrankCoder");
        em.persist(account);
        em.flush();

        Optional<LeetCodeAccount> found = accounts.findByLeetcodeUsernameIgnoreCase("frankcoder");
        assertThat(found).isPresent();
        assertThat(found.get().getDiscordUser().getDiscordUserId()).isEqualTo("123456789012345688");

        Optional<LeetCodeAccount> foundUpper = accounts.findByLeetcodeUsernameIgnoreCase("FRANKCODER");
        assertThat(foundUpper).isPresent();
    }

    @Test
    void findsForVerificationByDiscordUserId() {
        DiscordUser user = new DiscordUser("123456789012345689", "grace");
        em.persist(user);
        LeetCodeAccount account = new LeetCodeAccount(user, "grace_lc");
        em.persist(account);
        em.flush();

        Optional<LeetCodeAccount> found = accounts.findForVerificationByDiscordUserId("123456789012345689");
        assertThat(found).isPresent();
        assertThat(found.get().getLeetcodeUsername()).isEqualTo("grace_lc");
    }
}
