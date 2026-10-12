package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class DiscordReadyListenerTests {

    @Test
    void logsTheReadyBotsIdentity(CapturedOutput output) {
        JDA jda = mock(JDA.class);
        ReadyEvent event = mock(ReadyEvent.class);
        SelfUser bot = mock(SelfUser.class);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(bot);
        when(bot.getName()).thenReturn("DailyCode-test");
        when(bot.getId()).thenReturn("123456789");

        new DiscordReadyListener().onReady(event);

        assertThat(output.getOut()).contains("Discord bot ready: DailyCode-test (id=123456789).");
    }
}
