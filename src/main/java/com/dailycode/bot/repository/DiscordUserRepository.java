package com.dailycode.bot.repository;

import com.dailycode.bot.domain.user.DiscordUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {

    Optional<DiscordUser> findByDiscordUserId(String discordUserId);
}
