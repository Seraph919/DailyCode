package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeParticipantDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeJoinService {

    private final ChallengeJoinTransaction joinTransaction;

    @Transactional(propagation = Propagation.NEVER)
    public ChallengeParticipantDto join(
            @NotNull @Positive Long challengeId,
            @NotBlank @Size(max = 20) String discordGuildId,
            @NotBlank @Size(max = 20) String discordUserId) {
        try {
            return joinTransaction.join(challengeId, discordGuildId.strip(), discordUserId.strip());
        } catch (DataIntegrityViolationException failure) {
            // The separate transactional bean has already rolled back before we translate the error.
            if (isDuplicateMembership(failure)) {
                throw new ChallengeJoinException(ChallengeJoinException.Reason.ALREADY_JOINED);
            }
            throw failure;
        }
    }

    private boolean isDuplicateMembership(DataIntegrityViolationException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "uk_challenge_participants_challenge_user".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
