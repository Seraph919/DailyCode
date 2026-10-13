package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.leetcode.dto.GraphQlRequestDto;
import com.dailycode.bot.leetcode.dto.LeetCodeMatchedUserDto;
import com.dailycode.bot.leetcode.dto.LeetCodeProfileResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class LeetCodeClient {

    private static final String PROFILE_QUERY = """
            query GetUserProfile($username: String!) {
              matchedUser(username: $username) {
                username
                profile {
                  ranking
                }
              }
            }
            """;

    private final WebClient webClient;

    private static final String VERIFICATION_PROFILE_QUERY = """
            query GetVerificationProfile($username: String!) {
              matchedUser(username: $username) {
                username
                profile {
                  aboutMe
                }
              }
            }
            """;

    public LeetCodeClient(@Qualifier("leetCodeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public LeetCodeUserProfileDto fetchProfile(String username) {
        LeetCodeMatchedUserDto user = fetchMatchedUser(username, "GetUserProfile", PROFILE_QUERY);
        LeetCodeUserProfileDto profile = new LeetCodeUserProfileDto();
        profile.setUsername(user.getUsername());
        profile.setRanking(user.getProfile().getRanking());
        return profile;
    }

    public LeetCodeVerificationProfileDto fetchVerificationProfile(String username) {
        LeetCodeMatchedUserDto user = fetchMatchedUser(username, "GetVerificationProfile", VERIFICATION_PROFILE_QUERY);
        if (!user.getUsername().equalsIgnoreCase(username.strip())) {
            throw new LeetCodeClientException("LeetCode returned a different username.");
        }
        if (user.getProfile().getAboutMe() == null) {
            throw new LeetCodeClientException("LeetCode returned no verification profile text.");
        }

        return new LeetCodeVerificationProfileDto(user.getUsername(), user.getProfile().getAboutMe());
    }

    private LeetCodeMatchedUserDto fetchMatchedUser(String username, String operationName, String query) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("LeetCode username must not be blank.");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username.strip());

        GraphQlRequestDto request = new GraphQlRequestDto();
        request.setOperationName(operationName);
        request.setQuery(query);
        request.setVariables(variables);

        LeetCodeProfileResponseDto response;
        try {
            response = webClient.post()
                    .uri("/graphql/")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LeetCodeProfileResponseDto.class)
                    .block(Duration.ofSeconds(10));
        } catch (RuntimeException failure) {
            throw new LeetCodeClientException("Could not fetch the LeetCode profile.", failure);
        }

        if (response == null) {
            throw new LeetCodeClientException("LeetCode returned an empty response.");
        }
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new LeetCodeClientException("LeetCode returned GraphQL errors.");
        }
        if (response.getData() == null) {
            throw new LeetCodeClientException("LeetCode returned no profile data.");
        }

        LeetCodeMatchedUserDto user = response.getData().getMatchedUser();
        if (user == null) {
            throw new LeetCodeClientException("LeetCode did not return a matching user.");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new LeetCodeClientException("LeetCode returned a user without a username.");
        }
        if (user.getProfile() == null) {
            throw new LeetCodeClientException("LeetCode returned a user without a profile.");
        }

        return user;
    }
}
