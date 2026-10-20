package com.dailycode.bot.application.challenge.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AutomaticCheckBatchDto {
    // Null means there are no more participants, even if the previous page had no targets.
    private final Long nextParticipantId;
    private final List<AutomaticCheckTargetDto> targets;
}
