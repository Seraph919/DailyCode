package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeLinkChallengeDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class LeetCodeLinkService {

    private final LeetCodeClient client;
    private final LeetCodeLinkTransaction linkTransaction;

    public LeetCodeLinkChallengeDto startLink(
            @NotBlank @Size(max = 20) String discordUserId,
            @NotBlank @Size(max = 100) String leetcodeUsername) {
        LeetCodeUserProfileDto profile = client.fetchProfile(leetcodeUsername);
        String returnedUsername = profile.getUsername();
        if (returnedUsername == null || returnedUsername.isBlank() || returnedUsername.length() > 100) {
            throw new LeetCodeClientException("LeetCode returned an invalid username.");
        }
        if (!returnedUsername.equalsIgnoreCase(leetcodeUsername.strip())) {
            throw new LeetCodeClientException("LeetCode returned a different username.");
        }

        return linkTransaction.createChallenge(discordUserId, returnedUsername);
    }
}
