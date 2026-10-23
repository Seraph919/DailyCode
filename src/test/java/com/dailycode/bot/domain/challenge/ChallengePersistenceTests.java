package com.dailycode.bot.domain.challenge;

import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ChallengePersistenceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    @Test
    void persistsChallengeAndSetsDefaults() {
        DiscordUser creator = new DiscordUser("123456789012345601", "creator1");
        em.persist(creator);
        em.flush();

        Challenge challenge = new Challenge("100", "Spring Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "Africa/Casablanca");
        em.persist(challenge);
        em.flush();

        Long id = challenge.getId();
        assertThat(id).isNotNull();
        assertThat(challenge.getCreatedAt()).isNotNull();
        assertThat(challenge.getUpdatedAt()).isNotNull();
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.SCHEDULED);
        assertThat(challenge.getAutomationDifficulty()).isEqualTo(ProblemDifficulty.EASY);
        assertThat(challenge.getAutomationTime()).isEqualTo(LocalTime.MIDNIGHT);

        em.clear();
        Challenge loaded = em.find(Challenge.class, id);
        assertThat(loaded.getName()).isEqualTo("Spring Practice");
        assertThat(loaded.getTimezone()).isEqualTo("Africa/Casablanca");
    }

    @Test
    void challengeAutomateAndStopAutomation() {
        DiscordUser creator = new DiscordUser("123456789012345602", "creator2");
        em.persist(creator);
        Challenge challenge = new Challenge("100", "Automation Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);
        em.flush();

        challenge.automate("200", ProblemDifficulty.MEDIUM, LocalTime.of(18, 30), "300");
        em.flush();
        em.clear();

        Challenge loaded = em.find(Challenge.class, challenge.getId());
        assertThat(loaded.getAnnouncementChannelId()).isEqualTo("200");
        assertThat(loaded.getAutomationDifficulty()).isEqualTo(ProblemDifficulty.MEDIUM);
        assertThat(loaded.getAutomationTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(loaded.getAnnouncementRoleId()).isEqualTo("300");

        loaded.stopAutomation();
        em.flush();
        em.clear();

        Challenge stopped = em.find(Challenge.class, challenge.getId());
        assertThat(stopped.getAnnouncementChannelId()).isNull();
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        DiscordUser creator = new DiscordUser("123456789012345603", "creator3");
        assertThatThrownBy(() -> new Challenge("100", "Invalid", creator,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), "UTC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must not be before start date.");
    }

    @Test
    void persistsParticipantAndEnforcesUniqueness() {
        DiscordUser creator = new DiscordUser("123456789012345604", "creator4");
        DiscordUser member = new DiscordUser("123456789012345605", "member1");
        em.persist(creator);
        em.persist(member);
        Challenge challenge = new Challenge("100", "Join Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);
        em.flush();

        ChallengeParticipant participant1 = new ChallengeParticipant(challenge, member);
        em.persist(participant1);
        em.flush();

        assertThat(participant1.getId()).isNotNull();
        assertThat(participant1.getJoinedAt()).isNotNull();

        // Duplicate join should violate uk_challenge_participants_challenge_user
        ChallengeParticipant participant2 = new ChallengeParticipant(challenge, member);
        assertThatThrownBy(() -> em.persist(participant2))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_challenge_participants_challenge_user");
    }

    @Test
    void persistsProblemAndEnforcesSlugUniqueness() {
        Problem problem1 = new Problem("1", "1", "Two Sum", "two-sum", ProblemDifficulty.EASY, false);
        em.persist(problem1);
        em.flush();

        assertThat(problem1.getId()).isNotNull();
        assertThat(problem1.getCreatedAt()).isNotNull();

        Problem problem2 = new Problem("2", "2", "Two Sum Duplicate", "two-sum", ProblemDifficulty.EASY, false);
        assertThatThrownBy(() -> em.persist(problem2))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_problems_title_slug");
    }

    @Test
    void persistsDailyChallengeAndEnforcesUniqueness() {
        DiscordUser creator = new DiscordUser("123456789012345606", "creator6");
        em.persist(creator);
        Challenge challenge = new Challenge("100", "Daily Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);

        Problem problem1 = new Problem("10", "10", "Problem 10", "problem-10", ProblemDifficulty.EASY, false);
        Problem problem2 = new Problem("11", "11", "Problem 11", "problem-11", ProblemDifficulty.EASY, false);
        em.persist(problem1);
        em.persist(problem2);
        em.flush();

        LocalDate date = LocalDate.of(2026, 1, 5);
        DailyChallenge daily1 = new DailyChallenge(challenge, problem1, date);
        em.persist(daily1);
        em.flush();

        assertThat(daily1.getId()).isNotNull();
        assertThat(daily1.getAssignedAt()).isNotNull();

        // Duplicate assignment for the same day violates uk_daily_challenges_day
        DailyChallenge dailyDuplicateDay = new DailyChallenge(challenge, problem2, date);
        assertThatThrownBy(() -> em.persist(dailyDuplicateDay))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_daily_challenges_day");
    }

    @Test
    void persistsDailyProgressAndRecordsCompletion() {
        DiscordUser creator = new DiscordUser("123456789012345607", "creator7");
        DiscordUser member = new DiscordUser("123456789012345608", "member7");
        em.persist(creator);
        em.persist(member);
        Challenge challenge = new Challenge("100", "Progress Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);
        ChallengeParticipant participant = new ChallengeParticipant(challenge, member);
        em.persist(participant);
        Problem problem = new Problem("20", "20", "Problem 20", "problem-20", ProblemDifficulty.EASY, false);
        em.persist(problem);
        DailyChallenge daily = new DailyChallenge(challenge, problem, LocalDate.of(2026, 1, 5));
        em.persist(daily);
        em.flush();

        DailyProgress progress = new DailyProgress(participant, daily);
        em.persist(progress);
        em.flush();

        assertThat(progress.getId()).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(DailyProgressStatus.PENDING);

        // Duplicate progress violates uk_daily_progress_participant_day
        DailyProgress duplicateProgress = new DailyProgress(participant, daily);
        assertThatThrownBy(() -> em.persist(duplicateProgress))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("uk_daily_progress_participant_day");
    }
}
