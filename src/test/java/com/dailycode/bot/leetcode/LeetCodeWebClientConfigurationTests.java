package com.dailycode.bot.leetcode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LeetCodeWebClientConfigurationTests {

    @Test
    void sendsJsonToTheConfiguredHostWithoutAuthenticationHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ProbeHandler handler = new ProbeHandler();
        server.createContext("/probe", handler);
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            LeetCodeWebClientConfiguration configuration = new LeetCodeWebClientConfiguration();
            WebClient client = configuration.leetCodeWebClient(WebClient.builder(), baseUrl);

            // This is a local HTTP probe, not a LeetCode GraphQL request.
            String result = client.post()
                    .uri("/probe")
                    .bodyValue("{\"probe\":true}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            HttpExchange request = handler.requests.poll(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestMethod()).isEqualTo("POST");
            assertThat(request.getRequestURI().getPath()).isEqualTo("/probe");
            assertThat(request.getRequestHeaders().getFirst("Accept")).isEqualTo("application/json");
            assertThat(request.getRequestHeaders().getFirst("Content-Type")).isEqualTo("application/json");
            assertThat(request.getRequestHeaders().getFirst("Authorization")).isNull();
            assertThat(request.getRequestHeaders().getFirst("Cookie")).isNull();
            assertThat(handler.bodies.poll(1, TimeUnit.SECONDS)).isEqualTo("{\"probe\":true}");
            assertThat(result).isEqualTo("{\"ok\":true}");
        } finally {
            server.stop(0);
        }
    }

    private static class ProbeHandler implements HttpHandler {

        private final BlockingQueue<HttpExchange> requests = new ArrayBlockingQueue<>(1);
        private final BlockingQueue<String> bodies = new ArrayBlockingQueue<>(1);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                bodies.add(body);
                requests.add(exchange);
                byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        }
    }
}
