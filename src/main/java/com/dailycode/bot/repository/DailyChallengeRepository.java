package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.DailyChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    @Query("""
            select daily.assignmentDate from DailyChallenge daily
            where daily.challenge.id = :challengeId and daily.challenge.discordGuildId = :guildId
              and daily.assignmentDate <= :today
            order by daily.assignmentDate
            """)
    List<LocalDate> findAssignedDates(@Param("challengeId") Long challengeId,
                                     @Param("guildId") String guildId, @Param("today") LocalDate today);

    @Query("""
            select count(daily) > 0 from DailyChallenge daily
            where daily.challenge.id = :challengeId and daily.assignmentDate <= :today
              and not exists (select progress.id from DailyProgress progress
                              where progress.dailyChallenge = daily and progress.participant.id = :participantId
                                and progress.status = com.dailycode.bot.domain.challenge.DailyProgressStatus.COMPLETED)
            """)
    boolean hasUncompletedAssignments(@Param("challengeId") Long challengeId,
                                      @Param("participantId") Long participantId, @Param("today") LocalDate today);

    Optional<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
            Long challengeId, String guildId, LocalDate assignmentDate);

    Page<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDateLessThanEqualOrderByAssignmentDateDesc(
            Long challengeId, String guildId, LocalDate date, Pageable pageable);

    Page<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdOrderByAssignmentDateAsc(
            Long challengeId, String guildId, Pageable pageable);
}
