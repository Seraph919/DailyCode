package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.leetcode.dto.LeetCodeAcceptedSubmissionDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.domain.challenge.*;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.problem.*;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({PostgresTestConfiguration.class, ChallengeCompletionPersistenceTests.Time.class})
@SpringBootTest(properties = {"spring.profiles.active=test", "spring.jpa.show-sql=false"})
class ChallengeCompletionPersistenceTests {
    @TestConfiguration
    static class Time {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC); }
    }
    private static final AtomicLong IDS = new AtomicLong(6000);
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired DailyCompletionService completion;
    @Autowired ChallengeProgressService progress;
    @Autowired AutomaticCheckTargets targets;
    @Autowired ChallengeAutomationService automation;
    @MockitoBean LeetCodeClient client;

    record Fixture(long id, String user, String username, String firstSlug, String secondSlug) {}

    private Fixture fixture() {
        long unique = IDS.incrementAndGet();
        return new TransactionTemplate(manager).execute(tx -> {
            var user = new DiscordUser("" + unique, "coder");
            em.persist(user);
            var account = new LeetCodeAccount(user, "coder" + unique);
            var verifiedAt = Instant.parse("2026-09-01T12:00:00Z");
            account.startVerification("proof", verifiedAt.plusSeconds(60), verifiedAt);
            account.completeVerification("proof", verifiedAt);
            em.persist(account);
            var challenge = new Challenge("100", "Past challenge", user, LocalDate.of(2026,10,1), LocalDate.of(2026,10,10), "UTC");
            challenge.automate("12", ProblemDifficulty.EASY, LocalTime.NOON, null);
            em.persist(challenge);
            var member = new ChallengeParticipant(challenge, user);
            em.persist(member);
            var p1 = new Problem(unique + "1", unique + "1", "First", "first-" + unique, ProblemDifficulty.EASY, false);
            var p2 = new Problem(unique + "2", unique + "2", "Second", "second-" + unique, ProblemDifficulty.EASY, false);
            em.persist(p1); em.persist(p2);
            em.persist(new DailyChallenge(challenge, p1, LocalDate.of(2026,10,1)));
            em.persist(new DailyChallenge(challenge, p2, LocalDate.of(2026,10,3)));
            em.flush();
            em.createNativeQuery("update challenge_participants set joined_at = '2026-10-02T12:00:00Z' where id = :id")
                    .setParameter("id", member.getId()).executeUpdate();
            em.createNativeQuery("update daily_challenges set assigned_at = assignment_date + time '12:00' where challenge_id = :id")
                    .setParameter("id", challenge.getId()).executeUpdate();
            return new Fixture(challenge.getId(), user.getDiscordUserId(), account.getLeetcodeUsername(), p1.getTitleSlug(), p2.getTitleSlug());
        });
    }

    private LeetCodeRecentSubmissionsDto feed(Fixture f, Instant submittedAt) {
        return new LeetCodeRecentSubmissionsDto(f.username(), List.of(
                new LeetCodeAcceptedSubmissionDto("101", "First", f.firstSlug(), submittedAt),
                new LeetCodeAcceptedSubmissionDto("102", "Second", f.secondSlug(), submittedAt)));
    }

    @Test
    void recordsLateWorkAcrossEndedChallengeAndPreservesEvidenceOnRepeatedChecks() {
        var f = fixture();
        when(client.fetchRecentAcceptedSubmissions(f.username())).thenReturn(feed(f, Instant.parse("2026-10-27T13:00:00Z")));
        assertThat(targets.next(0).getTargets()).anyMatch(t -> t.getChallengeId().equals(f.id()));
        var result = completion.checkAll(f.id(), "100", f.user());
        assertThat(result.checked()).isEqualTo(2);
        assertThat(result.newlyCompleted()).isEqualTo(2);
        var status = progress.status(f.id(), "100", f.user(), 1);
        assertThat(status.completed()).isEqualTo(2);
        assertThat(status.history()).allMatch(day -> day.status().equals("Completed (late)"));
        assertThat(completion.checkAll(f.id(), "100", f.user()).alreadyCompleted()).isEqualTo(2);
        verify(client, times(1)).fetchRecentAcceptedSubmissions(f.username());
        assertThat(targets.next(0).getTargets()).noneMatch(t -> t.getChallengeId().equals(f.id()));
    }

    @Test
    void rejectsCrossServerAndPreMembershipEvidence() {
        var f = fixture();
        assertThatThrownBy(() -> completion.checkAll(f.id(), "999", f.user()))
                .isInstanceOf(DailyCompletionException.class).hasMessage("CHALLENGE_NOT_FOUND");
        verifyNoInteractions(client);
        when(client.fetchRecentAcceptedSubmissions(f.username())).thenReturn(feed(f, Instant.parse("2026-10-01T14:00:00Z")));
        assertThat(completion.checkAll(f.id(), "100", f.user()).noMatch()).isEqualTo(2);
        assertThat(progress.status(f.id(), "100", f.user(), 1).completed()).isZero();
    }

    @Test
    void stoppingAutomationDuringFetchPreventsSavingCompletions() {
        var f = fixture();
        when(client.fetchRecentAcceptedSubmissions(f.username())).thenAnswer(call -> {
            automation.disable(f.id(), "100");
            return feed(f, Instant.parse("2026-10-27T13:00:00Z"));
        });
        assertThat(completion.checkAllAutomatic(f.id(), "100", f.user()).checked()).isZero();
        assertThat(progress.status(f.id(), "100", f.user(), 1).completed()).isZero();
    }
}
