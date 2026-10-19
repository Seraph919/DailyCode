package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.Challenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByIdAndDiscordGuildId(Long id, String discordGuildId);

    // Concurrent joins can share this lock; challenge updates must wait until they finish.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select challenge from Challenge challenge where challenge.id = :id and challenge.discordGuildId = :guildId")
    Optional<Challenge> findForJoin(@Param("id") Long id, @Param("guildId") String guildId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from Challenge challenge where challenge.id = :id and challenge.discordGuildId = :guildId")
    Optional<Challenge> findForAssignment(@Param("id") Long id, @Param("guildId") String guildId);

    Page<Challenge> findByDiscordGuildIdOrderByStartDateAscIdAsc(String discordGuildId, Pageable pageable);
}
