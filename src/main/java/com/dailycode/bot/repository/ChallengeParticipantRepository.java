package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {

    // Caller holds the scoped challenge lock. Lock only membership, not the joined user row.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select participant from ChallengeParticipant participant
            where participant.challenge.id = :challengeId
              and participant.discordUser.id = (
                select user.id from DiscordUser user where user.discordUserId = :discordUserId
              )
            """)
    Optional<ChallengeParticipant> findForCompletion(@Param("challengeId") Long challengeId,
                                                    @Param("discordUserId") String discordUserId);

    @Query("""
            select participant from ChallengeParticipant participant
            where participant.challenge.id = :challengeId
              and participant.challenge.discordGuildId = :guildId
              and participant.discordUser.discordUserId = :discordUserId
            """)
    Optional<ChallengeParticipant> findMembership(@Param("challengeId") Long challengeId,
                                                 @Param("guildId") String guildId,
                                                 @Param("discordUserId") String discordUserId);

    Page<ChallengeParticipant> findByChallenge_IdAndChallenge_DiscordGuildIdOrderByJoinedAtAscIdAsc(
            Long challengeId, String discordGuildId, Pageable pageable);
}
