package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.DailyProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DailyProgressRepository extends JpaRepository<DailyProgress, Long> {

    @Query("""
            select progress from DailyProgress progress
            where progress.dailyChallenge.id = :dailyChallengeId
              and progress.participant.challenge.discordGuildId = :guildId
              and progress.participant.discordUser.discordUserId = :discordUserId
            """)
    Optional<DailyProgress> findProgress(@Param("dailyChallengeId") Long dailyChallengeId,
                                       @Param("guildId") String guildId,
                                       @Param("discordUserId") String discordUserId);

    @Query("""
            select progress from DailyProgress progress
            where progress.challengeId = :challengeId
              and progress.participant.challenge.discordGuildId = :guildId
              and progress.participant.discordUser.discordUserId = :discordUserId
            order by progress.dailyChallenge.assignmentDate asc, progress.id asc
            """)
    Page<DailyProgress> findHistory(@Param("challengeId") Long challengeId,
                                   @Param("guildId") String guildId,
                                   @Param("discordUserId") String discordUserId,
                                   Pageable pageable);
}
