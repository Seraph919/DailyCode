package com.dailycode.bot.api.dev;

import com.dailycode.bot.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class DevelopmentEndpointsDisabledTests {

    @Test
    void developmentRoutesAreAbsentWithoutDevProfile(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/api/dev/discord-users").contentType("application/json")
                        .content("{\"discordUserId\":\"123\",\"discordUsername\":\"alice\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
