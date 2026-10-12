package com.dailycode.bot.api.dev;

import com.dailycode.bot.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test,dev")
@AutoConfigureMockMvc
@Transactional
class DiscordUserDevControllerTests {

    @Test
    void registersAndRefreshesThroughHttp(@Autowired MockMvc mvc) throws Exception {
        String body = """
                {"discordUserId":"123456789012345800","discordUsername":"alice"}
                """;
        String first = mvc.perform(post("/api/dev/discord-users")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.discordUsername").value("alice"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/dev/discord-users").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(content().json(first));
        mvc.perform(post("/api/dev/discord-users").contentType("application/json")
                        .content(body.replace("alice", "alice-updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discordUserId").value("123456789012345800"))
                .andExpect(jsonPath("$.discordUsername").value("alice-updated"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"discordUserId\":\"123\",\"discordUsername\":\" \"}",
            "{\"discordUserId\":\"123456789012345678901\",\"discordUsername\":\"alice\"}",
            "not-json"
    })
    void rejectsInvalidRequests(String body, @Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/api/dev/discord-users").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesOpenApiAndSwaggerUi(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/dev/discord-users'].post").exists())
                .andExpect(jsonPath("$.components.schemas.RegisterDiscordUserRequest.properties.discordUserId.maxLength")
                        .value(20));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
