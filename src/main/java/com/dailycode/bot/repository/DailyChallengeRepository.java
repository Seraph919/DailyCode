package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.DailyChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    Optional<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
            Long challengeId, String guildId, LocalDate assignmentDate);

    Page<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDateLessThanEqualOrderByAssignmentDateDesc(
            Long challengeId, String guildId, LocalDate date, Pageable pageable);

    Page<DailyChallenge> findByChallenge_IdAndChallenge_DiscordGuildIdOrderByAssignmentDateAsc(
            Long challengeId, String guildId, Pageable pageable);
}
