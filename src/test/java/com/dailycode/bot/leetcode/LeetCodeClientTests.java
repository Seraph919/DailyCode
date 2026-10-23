package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeCatalogPageDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeetCodeClientTests {

    private HttpServer server;
    private LeetCodeClient client;
    private final AtomicReference<String> responseBody = new AtomicReference<>("");
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> receivedRequestBody = new AtomicReference<>("");

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    receivedRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(responseStatus.get(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                } finally {
                    exchange.close();
                }
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        client = new LeetCodeClient(webClient);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- fetchProfile tests ---

    @Test
    void fetchProfileReturnsMappedData() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "Seraph919",
                      "profile": {
                        "ranking": 42500
                      }
                    }
                  }
                }
                """);

        LeetCodeUserProfileDto result = client.fetchProfile("Seraph919");
        assertThat(result.getUsername()).isEqualTo("Seraph919");
        assertThat(result.getRanking()).isEqualTo(42500);
        assertThat(receivedRequestBody.get()).contains("\"operationName\":\"GetUserProfile\"");
        assertThat(receivedRequestBody.get()).contains("\"username\":\"Seraph919\"");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void fetchProfileRejectsBlankUsername(String username) {
        assertThatThrownBy(() -> client.fetchProfile(username))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LeetCode username must not be blank.");
    }

    @Test
    void fetchProfileThrowsOnGraphQLErrors() {
        responseBody.set("""
                {
                  "errors": [{"message": "User not found"}],
                  "data": null
                }
                """);

        assertThatThrownBy(() -> client.fetchProfile("ghostUser"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned GraphQL errors.");
    }

    @Test
    void fetchProfileThrowsWhenUserNotFound() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": null
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchProfile("ghostUser"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode did not return a matching user.");
    }

    @Test
    void fetchProfileThrowsOnHttpFailure() {
        responseStatus.set(500);
        responseBody.set("Internal Server Error");

        assertThatThrownBy(() -> client.fetchProfile("Seraph919"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("Could not fetch the LeetCode profile.");
    }

    // --- fetchVerificationProfile tests ---

    @Test
    void fetchVerificationProfileReturnsAboutMe() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "Seraph919",
                      "profile": {
                        "aboutMe": "dailycode-token123\\nMy personal bio"
                      }
                    }
                  }
                }
                """);

        LeetCodeVerificationProfileDto result = client.fetchVerificationProfile("Seraph919");
        assertThat(result.getUsername()).isEqualTo("Seraph919");
        assertThat(result.getAboutMe()).isEqualTo("dailycode-token123\nMy personal bio");
    }

    @Test
    void fetchVerificationProfileThrowsWhenUsernameMismatched() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "OtherUser",
                      "profile": {
                        "aboutMe": "dailycode-token123"
                      }
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchVerificationProfile("Seraph919"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned a different username.");
    }

    @Test
    void fetchVerificationProfileThrowsWhenAboutMeMissing() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "Seraph919",
                      "profile": {
                        "aboutMe": null
                      }
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchVerificationProfile("Seraph919"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned no verification profile text.");
    }

    // --- fetchProblem tests ---

    @Test
    void fetchProblemReturnsMappedProblem() {
        responseBody.set("""
                {
                  "data": {
                    "question": {
                      "questionId": "1",
                      "questionFrontendId": "1",
                      "title": "Two Sum",
                      "titleSlug": "two-sum",
                      "difficulty": "Easy",
                      "isPaidOnly": false
                    }
                  }
                }
                """);

        LeetCodeProblemDto problem = client.fetchProblem("two-sum");
        assertThat(problem.getLeetCodeQuestionId()).isEqualTo("1");
        assertThat(problem.getFrontendId()).isEqualTo("1");
        assertThat(problem.getTitle()).isEqualTo("Two Sum");
        assertThat(problem.getTitleSlug()).isEqualTo("two-sum");
        assertThat(problem.getDifficulty()).isEqualTo(ProblemDifficulty.EASY);
        assertThat(problem.isPaidOnly()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Two Sum", "invalid_slug!", "two--sum"})
    void fetchProblemRejectsInvalidSlug(String slug) {
        assertThatThrownBy(() -> client.fetchProblem(slug))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide a valid LeetCode problem slug of at most 300 characters.");
    }

    @Test
    void fetchProblemThrowsWhenSlugMismatched() {
        responseBody.set("""
                {
                  "data": {
                    "question": {
                      "questionId": "2",
                      "questionFrontendId": "2",
                      "title": "Add Two Numbers",
                      "titleSlug": "add-two-numbers",
                      "difficulty": "Medium",
                      "isPaidOnly": false
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchProblem("two-sum"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned a different problem slug.");
    }

    @Test
    void fetchProblemThrowsWhenProblemNotFound() {
        responseBody.set("""
                {
                  "data": {
                    "question": null
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchProblem("two-sum"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode did not return a matching problem.");
    }

    // --- fetchRecentAcceptedSubmissions tests ---

    @Test
    void fetchRecentAcceptedSubmissionsReturnsMappedList() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "Seraph919"
                    },
                    "recentAcSubmissionList": [
                      {
                        "id": "123456",
                        "title": "Two Sum",
                        "titleSlug": "two-sum",
                        "timestamp": "1700000000"
                      }
                    ]
                  }
                }
                """);

        LeetCodeRecentSubmissionsDto result = client.fetchRecentAcceptedSubmissions("Seraph919");
        assertThat(result.getUsername()).isEqualTo("Seraph919");
        assertThat(result.getSubmissions()).hasSize(1);
        var sub = result.getSubmissions().getFirst();
        assertThat(sub.getSubmissionId()).isEqualTo("123456");
        assertThat(sub.getTitle()).isEqualTo("Two Sum");
        assertThat(sub.getTitleSlug()).isEqualTo("two-sum");
        assertThat(sub.getSubmittedAt()).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void fetchRecentAcceptedSubmissionsThrowsOnDuplicateSubmissionIds() {
        responseBody.set("""
                {
                  "data": {
                    "matchedUser": {
                      "username": "Seraph919"
                    },
                    "recentAcSubmissionList": [
                      {
                        "id": "123456",
                        "title": "Two Sum",
                        "titleSlug": "two-sum",
                        "timestamp": "1700000000"
                      },
                      {
                        "id": "123456",
                        "title": "Two Sum Again",
                        "titleSlug": "two-sum",
                        "timestamp": "1700000100"
                      }
                    ]
                  }
                }
                """);

        assertThatThrownBy(() -> client.fetchRecentAcceptedSubmissions("Seraph919"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned duplicate submission IDs.");
    }

    // --- fetchCatalog tests ---

    @Test
    void fetchCatalogReturnsPageData() {
        responseBody.set("""
                {
                  "data": {
                    "problemsetQuestionList": {
                      "total": 100,
                      "questions": [
                        {
                          "titleSlug": "two-sum",
                          "difficulty": "Easy",
                          "isPaidOnly": false
                        }
                      ]
                    }
                  }
                }
                """);

        LeetCodeCatalogPageDto result = client.fetchCatalog(ProblemDifficulty.EASY, 0, 10);
        assertThat(result.total()).isEqualTo(100);
        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().getFirst().titleSlug()).isEqualTo("two-sum");
        assertThat(result.entries().getFirst().paidOnly()).isFalse();
    }

    @Test
    void fetchCatalogRejectsInvalidArguments() {
        assertThatThrownBy(() -> client.fetchCatalog(null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.fetchCatalog(ProblemDifficulty.EASY, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.fetchCatalog(ProblemDifficulty.EASY, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.fetchCatalog(ProblemDifficulty.EASY, 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
