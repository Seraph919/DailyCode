package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ChallengeRepositoryTests {

    @Autowired
    private ChallengeRepository challenges;

    @Autowired
    private EntityManager em;

    @Test
    void findsByIdAndGuildId() {
        DiscordUser creator = new DiscordUser("123456789012345610", "creator10");
        em.persist(creator);
        Challenge challenge = new Challenge("100", "Guild Challenge", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);
        em.flush();

        Optional<Challenge> found = challenges.findByIdAndDiscordGuildId(challenge.getId(), "100");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Guild Challenge");

        assertThat(challenges.findByIdAndDiscordGuildId(challenge.getId(), "999")).isEmpty();
    }

    @Test
    void findsForJoinAndForAssignment() {
        DiscordUser creator = new DiscordUser("123456789012345611", "creator11");
        em.persist(creator);
        Challenge challenge = new Challenge("100", "Lock Challenge", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);
        em.flush();

        assertThat(challenges.findForJoin(challenge.getId(), "100")).isPresent();
        assertThat(challenges.findForAssignment(challenge.getId(), "100")).isPresent();
    }

    @Test
    void findsAutomatedChallengesOrderedById() {
        DiscordUser creator = new DiscordUser("123456789012345612", "creator12");
        em.persist(creator);

        Challenge c1 = new Challenge("100", "C1", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        c1.automate("201", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, null);
        em.persist(c1);

        Challenge c2 = new Challenge("100", "C2", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        // Not automated
        em.persist(c2);

        Challenge c3 = new Challenge("100", "C3", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        c3.automate("203", ProblemDifficulty.HARD, LocalTime.NOON, null);
        em.persist(c3);
        em.flush();

        List<Challenge> automated = challenges.findByAnnouncementChannelIdIsNotNullAndIdGreaterThanOrderByIdAsc(
                0L, PageRequest.of(0, 10));

        assertThat(automated).extracting(Challenge::getId)
                .contains(c1.getId(), c3.getId())
                .doesNotContain(c2.getId());
    }
}
