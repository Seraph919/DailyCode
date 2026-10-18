package com.dailycode.bot.repository;

import com.dailycode.bot.domain.user.DiscordUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {

    Optional<DiscordUser> findByDiscordUserId(String discordUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from DiscordUser user where user.discordUserId = :discordUserId")
    Optional<DiscordUser> findForLinkByDiscordUserId(@Param("discordUserId") String discordUserId);
}
