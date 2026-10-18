package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByIdAndDiscordGuildId(Long id, String discordGuildId);

    Page<Challenge> findByDiscordGuildIdOrderByStartDateAscIdAsc(String discordGuildId, Pageable pageable);
}
