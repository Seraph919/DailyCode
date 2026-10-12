package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfile;
import com.dailycode.bot.leetcode.dto.GraphQlRequest;
import com.dailycode.bot.leetcode.dto.LeetCodeMatchedUser;
import com.dailycode.bot.leetcode.dto.LeetCodeProfileResponse;
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

    public LeetCodeClient(@Qualifier("leetCodeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public LeetCodeUserProfile fetchProfile(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("LeetCode username must not be blank.");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username.strip());

        GraphQlRequest request = new GraphQlRequest();
        request.setOperationName("GetUserProfile");
        request.setQuery(PROFILE_QUERY);
        request.setVariables(variables);

        LeetCodeProfileResponse response;
        try {
            response = webClient.post()
                    .uri("/graphql/")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LeetCodeProfileResponse.class)
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

        LeetCodeMatchedUser user = response.getData().getMatchedUser();
        if (user == null) {
            throw new LeetCodeClientException("LeetCode did not return a matching user.");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new LeetCodeClientException("LeetCode returned a user without a username.");
        }
        if (user.getProfile() == null) {
            throw new LeetCodeClientException("LeetCode returned a user without a profile.");
        }

        LeetCodeUserProfile profile = new LeetCodeUserProfile();
        profile.setUsername(user.getUsername());
        profile.setRanking(user.getProfile().getRanking());
        return profile;
    }
}
