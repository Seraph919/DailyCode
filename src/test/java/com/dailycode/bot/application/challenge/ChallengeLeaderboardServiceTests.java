package com.dailycode.bot.application.challenge;

import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.challenge.DailyProgress;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ChallengeLeaderboardServiceTests {
    @Autowired EntityManager entities;
    @Autowired ChallengeLeaderboardService service;
    private DiscordUser creator;
    private Challenge challenge;
    private DailyChallenge firstDay;
    private DailyChallenge secondDay;

    @BeforeEach
    void setup() {
        creator = user("10000");
        challenge = challenge("100");
        firstDay = day(challenge, "901", 1);
        secondDay = day(challenge, "902", 2);
    }

    @Test
    void ranksOnlyCompletedDaysIncludesZeroesAndSharesRanksForTies() {
        var first = participant(challenge, user("20001"));
        var tied = participant(challenge, user("20002"));
        var third = participant(challenge, user("20003"));
        var pending = participant(challenge, user("20004"));
        participant(challenge, user("20005"));
        completed(first, firstDay);
        completed(first, secondDay);
        completed(tied, firstDay);
        completed(tied, secondDay);
        completed(third, firstDay);
        entities.persist(new DailyProgress(pending, firstDay));
        // The same Discord user's completions in another server must not count here.
        var other = challenge("200");
        completed(participant(other, third.getDiscordUser()), day(other, "903", 1));
        entities.flush();
        entities.clear();

        var result = service.leaderboard(challenge.getId(), "100", 1);

        assertThat(result.participants()).isEqualTo(5);
        assertThat(result.entries()).extracting("discordUserId", "completed", "rank").containsExactly(
                tuple("20001", 2L, 1L), tuple("20002", 2L, 1L), tuple("20003", 1L, 3L),
                tuple("20004", 0L, 4L), tuple("20005", 0L, 4L));
    }

    @Test
    void paginatesStablyWithoutResettingRanksOrSplittingTiesIntoDifferentRanks() {
        var ids = new ArrayList<String>();
        for (int i = 0; i < 12; i++) {
            String id = String.valueOf(30000 + i);
            ids.add(id);
            var member = participant(challenge, user(id));
            if (i == 0) completed(member, firstDay);
        }
        entities.flush();
        var first = service.leaderboard(challenge.getId(), "100", 1);
        var second = service.leaderboard(challenge.getId(), "100", 2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.participants()).isEqualTo(12);
        assertThat(first.entries()).extracting("discordUserId").containsExactlyElementsOf(ids.subList(0, 10));
        assertThat(first.entries().get(9).rank()).isEqualTo(2);
        assertThat(second.entries()).extracting("discordUserId").containsExactlyElementsOf(ids.subList(10, 12));
        assertThat(second.entries()).extracting("rank").containsOnly(2L);
        var outOfRange = service.leaderboard(challenge.getId(), "100", 3);
        assertThat(outOfRange.totalPages()).isEqualTo(2);
        assertThat(outOfRange.entries()).isEmpty();
    }

    @Test
    void emptyChallengeHasOneEmptyPageAndDoesNotRequireMembership() {
        var result = service.leaderboard(challenge.getId(), "100", 1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.participants()).isZero();
        assertThat(result.entries()).isEmpty();
    }

    @Test
    void rejectsAnotherServersChallengeAndMissingChallenges() {
        assertThatThrownBy(() -> service.leaderboard(challenge.getId(), "200", 1))
                .isInstanceOfSatisfying(DailyCompletionException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> service.leaderboard(Long.MAX_VALUE, "100", 1))
                .isInstanceOf(DailyCompletionException.class);
    }

    @Test
    void validatesPageBounds() {
        assertThatThrownBy(() -> service.leaderboard(challenge.getId(), "100", 0))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.leaderboard(challenge.getId(), "100", 1000001))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private DiscordUser user(String id) {
        var user = new DiscordUser(id, "user-" + id);
        entities.persist(user);
        return user;
    }

    private Challenge challenge(String guild) {
        var value = new Challenge(guild, "Practice", creator,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "UTC");
        entities.persist(value);
        return value;
    }

    private DailyChallenge day(Challenge owner, String problemId, int day) {
        var problem = new Problem(problemId, problemId, "Problem", "problem-" + problemId, ProblemDifficulty.EASY, false);
        entities.persist(problem);
        var assignment = new DailyChallenge(owner, problem, LocalDate.of(2026, 10, day));
        entities.persist(assignment);
        return assignment;
    }

    private ChallengeParticipant participant(Challenge owner, DiscordUser user) {
        var participant = new ChallengeParticipant(owner, user);
        entities.persist(participant);
        return participant;
    }

    private void completed(ChallengeParticipant member, DailyChallenge day) {
        var record = new DailyProgress(member, day);
        var when = Instant.parse("2026-10-02T12:00:00Z");
        record.complete("submission-" + member.getId() + "-" + day.getId(), when, when);
        entities.persist(record);
    }
}
