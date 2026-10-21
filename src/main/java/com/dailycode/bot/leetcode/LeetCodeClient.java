package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeAcceptedSubmissionDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeCatalogPageDto;
import com.dailycode.bot.leetcode.dto.LeetCodeCatalogResponseDto;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.leetcode.dto.GraphQlRequestDto;
import com.dailycode.bot.leetcode.dto.LeetCodeMatchedUserDto;
import com.dailycode.bot.leetcode.dto.LeetCodeProfileResponseDto;
import com.dailycode.bot.leetcode.dto.LeetCodeProblemResponseDto;
import com.dailycode.bot.leetcode.dto.LeetCodeQuestionDto;
import com.dailycode.bot.leetcode.dto.LeetCodeRecentSubmissionsResponseDto;
import com.dailycode.bot.leetcode.dto.LeetCodeSubmissionDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component
public class LeetCodeClient {

    private static final String CATALOG_QUERY = """
            query DailyCodeCatalog($skip: Int!, $limit: Int!, $filters: QuestionListFilterInput) {
              problemsetQuestionList: questionList(categorySlug: "", skip: $skip, limit: $limit, filters: $filters) {
                total: totalNum
                questions: data {
                  titleSlug
                  difficulty
                  isPaidOnly
                }
              }
            }
            """;

    private static final int RECENT_SUBMISSION_LIMIT = 20;
    private static final String RECENT_SUBMISSIONS_QUERY = """
            query GetRecentAcceptedSubmissions($username: String!, $limit: Int!) {
              matchedUser(username: $username) {
                username
              }
              recentAcSubmissionList(username: $username, limit: $limit) {
                id
                title
                titleSlug
                timestamp
              }
            }
            """;

    private static final String PROBLEM_QUERY = """
            query GetProblemMetadata($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                questionId
                questionFrontendId
                title
                titleSlug
                difficulty
                isPaidOnly
              }
            }
            """;

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

    // A bounded recent feed, not complete submission history. Absence is not proof of failure.
    public LeetCodeRecentSubmissionsDto fetchRecentAcceptedSubmissions(String username) {
        if (username == null || username.isBlank() || username.strip().length() > 100) {
            throw new IllegalArgumentException("LeetCode username must contain 1 to 100 nonblank characters.");
        }
        String requestedUsername = username.strip();
        GraphQlRequestDto request = new GraphQlRequestDto("GetRecentAcceptedSubmissions",
                RECENT_SUBMISSIONS_QUERY, Map.of("username", requestedUsername, "limit", RECENT_SUBMISSION_LIMIT));
        LeetCodeRecentSubmissionsResponseDto response;
        try {
            response = webClient.post().uri("/graphql/").bodyValue(request).retrieve()
                    .bodyToMono(LeetCodeRecentSubmissionsResponseDto.class).block(Duration.ofSeconds(10));
        } catch (RuntimeException failure) {
            throw new LeetCodeClientException("Could not fetch recent accepted LeetCode submissions.", failure);
        }
        if (response == null) {
            throw new LeetCodeClientException("LeetCode returned an empty response.");
        }
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new LeetCodeClientException("LeetCode returned GraphQL errors.");
        }
        var data = response.getData();
        if (data == null || data.getMatchedUser() == null) {
            throw new LeetCodeClientException("LeetCode did not return a matching user.");
        }
        String returnedUsername = data.getMatchedUser().getUsername();
        if (!requestedUsername.equalsIgnoreCase(returnedUsername)) {
            throw new LeetCodeClientException("LeetCode returned a different username.");
        }
        var submissions = data.getRecentAcSubmissionList();
        if (submissions == null || submissions.size() > RECENT_SUBMISSION_LIMIT) {
            throw new LeetCodeClientException("LeetCode returned an invalid recent submission list.");
        }
        var results = new ArrayList<LeetCodeAcceptedSubmissionDto>();
        var seenIds = new HashSet<String>();
        for (LeetCodeSubmissionDto submission : submissions) {
            LeetCodeAcceptedSubmissionDto mapped = mapAcceptedSubmission(submission);
            if (!seenIds.add(mapped.getSubmissionId())) {
                throw new LeetCodeClientException("LeetCode returned duplicate submission IDs.");
            }
            results.add(mapped);
        }
        return new LeetCodeRecentSubmissionsDto(returnedUsername, results);
    }

    private LeetCodeAcceptedSubmissionDto mapAcceptedSubmission(LeetCodeSubmissionDto submission) {
        if (submission == null || submission.getId() == null
                || !submission.getId().matches("[1-9][0-9]{0,49}")) {
            throw new LeetCodeClientException("LeetCode returned an invalid submission ID.");
        }
        String title = submission.getTitle();
        if (title == null || title.isBlank() || title.length() > 300) {
            throw new LeetCodeClientException("LeetCode returned an invalid submission title.");
        }
        String slug = submission.getTitleSlug();
        if (slug == null || slug.length() > 300 || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new LeetCodeClientException("LeetCode returned an invalid submission problem slug.");
        }
        String timestamp = submission.getTimestamp();
        if (timestamp == null || !timestamp.matches("[0-9]{1,19}")) {
            throw new LeetCodeClientException("LeetCode returned an invalid submission timestamp.");
        }
        Instant submittedAt;
        try {
            submittedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException | DateTimeException failure) {
            throw new LeetCodeClientException("LeetCode returned an invalid submission timestamp.", failure);
        }
        return new LeetCodeAcceptedSubmissionDto(submission.getId(), title.strip(), slug, submittedAt);
    }

    public LeetCodeCatalogPageDto fetchCatalog(ProblemDifficulty difficulty, int skip, int limit) {
        if (difficulty == null || skip < 0 || limit < 1 || limit > 50) {
            throw new IllegalArgumentException("A difficulty, nonnegative offset and limit from 1 to 50 are required.");
        }
        var request = new GraphQlRequestDto("DailyCodeCatalog", CATALOG_QUERY,
                Map.of("skip", skip, "limit", limit, "filters", Map.of("difficulty", difficulty.name())));
        LeetCodeCatalogResponseDto response;
        try {
            response = webClient.post().uri("/graphql/").bodyValue(request).retrieve()
                    .bodyToMono(LeetCodeCatalogResponseDto.class).block(Duration.ofSeconds(10));
        } catch (RuntimeException failure) {
            throw new LeetCodeClientException("Could not fetch the LeetCode problem catalog.", failure);
        }
        if (response == null || response.errors() != null && !response.errors().isEmpty()
                || response.data() == null || response.data().problemsetQuestionList() == null) {
            throw new LeetCodeClientException("LeetCode returned no valid problem catalog.");
        }
        var page = response.data().problemsetQuestionList();
        if (page.total() == null || page.total() < 0 || page.questions() == null
                || page.questions().size() > limit
                || !page.questions().isEmpty() && (long) skip + page.questions().size() > page.total()
                || page.questions().isEmpty() && skip < page.total()) {
            throw new LeetCodeClientException("LeetCode returned invalid catalog pagination.");
        }
        var entries = new ArrayList<LeetCodeCatalogPageDto.Entry>();
        var slugs = new HashSet<String>();
        for (var question : page.questions()) {
            if (question == null || question.titleSlug() == null || question.titleSlug().length() > 300
                    || !question.titleSlug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")
                    || question.isPaidOnly() == null || !difficulty.name().equalsIgnoreCase(question.difficulty())
                    || !slugs.add(question.titleSlug())) {
                throw new LeetCodeClientException("LeetCode returned an invalid catalog entry.");
            }
            entries.add(new LeetCodeCatalogPageDto.Entry(question.titleSlug(), question.isPaidOnly()));
        }
        return new LeetCodeCatalogPageDto(page.total(), entries);
    }

    public LeetCodeProblemDto fetchProblem(String titleSlug) {
        if (titleSlug == null || titleSlug.strip().length() > 300
                || !titleSlug.strip().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Provide a valid LeetCode problem slug of at most 300 characters.");
        }
        String requestedSlug = titleSlug.strip();
        GraphQlRequestDto request = new GraphQlRequestDto(
                "GetProblemMetadata", PROBLEM_QUERY, Map.of("titleSlug", requestedSlug));

        LeetCodeProblemResponseDto response;
        try {
            response = webClient.post()
                    .uri("/graphql/")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LeetCodeProblemResponseDto.class)
                    .block(Duration.ofSeconds(10));
        } catch (RuntimeException failure) {
            throw new LeetCodeClientException("Could not fetch the LeetCode problem.", failure);
        }

        if (response == null) {
            throw new LeetCodeClientException("LeetCode returned an empty response.");
        }
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new LeetCodeClientException("LeetCode returned GraphQL errors.");
        }
        if (response.getData() == null || response.getData().getQuestion() == null) {
            throw new LeetCodeClientException("LeetCode did not return a matching problem.");
        }

        LeetCodeQuestionDto question = response.getData().getQuestion();
        if (!requestedSlug.equals(question.getTitleSlug())) {
            throw new LeetCodeClientException("LeetCode returned a different problem slug.");
        }
        String questionId = requireProblemText(question.getQuestionId(), "question ID", 50);
        String frontendId = requireProblemText(question.getQuestionFrontendId(), "frontend ID", 50);
        String title = requireProblemText(question.getTitle(), "title", 300);
        if (question.getIsPaidOnly() == null) {
            throw new LeetCodeClientException("LeetCode returned no problem premium status.");
        }
        ProblemDifficulty difficulty = switch (question.getDifficulty()) {
            case "Easy" -> ProblemDifficulty.EASY;
            case "Medium" -> ProblemDifficulty.MEDIUM;
            case "Hard" -> ProblemDifficulty.HARD;
            case null, default -> throw new LeetCodeClientException("LeetCode returned an unsupported problem difficulty.");
        };
        return new LeetCodeProblemDto(questionId, frontendId, title, requestedSlug,
                difficulty, question.getIsPaidOnly());
    }

    private static String requireProblemText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new LeetCodeClientException("LeetCode returned an invalid problem " + field + ".");
        }
        return value.strip();
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
