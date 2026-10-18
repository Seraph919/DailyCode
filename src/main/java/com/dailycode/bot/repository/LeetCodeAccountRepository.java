package com.dailycode.bot.repository;

import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeetCodeAccountRepository extends JpaRepository<LeetCodeAccount, Long> {

    Optional<LeetCodeAccount> findByDiscordUser_DiscordUserId(String discordUserId);

    // Lock only the account row: linking already holds the user lock, while
    // verification must not acquire a user lock in the opposite order.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account from LeetCodeAccount account
            where account.discordUser.id = (
                select user.id from DiscordUser user where user.discordUserId = :discordUserId
            )
            """)
    Optional<LeetCodeAccount> findForVerificationByDiscordUserId(@Param("discordUserId") String discordUserId);

    // Used LOWER to match V2's username uniqueness rule and expression.
    @Query("""
            select account from LeetCodeAccount account
            where lower(account.leetcodeUsername) = lower(:username)
            """)
    Optional<LeetCodeAccount> findByLeetcodeUsernameIgnoreCase(@Param("username") String username);
}
