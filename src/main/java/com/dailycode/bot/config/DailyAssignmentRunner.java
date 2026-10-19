package com.dailycode.bot.config;

import com.dailycode.bot.application.challenge.DailyAssignmentService;
import com.dailycode.bot.application.challenge.dto.DailyAssignmentDto;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("daily-assign")
public class DailyAssignmentRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DailyAssignmentRunner.class);
    private final DailyAssignmentService assignments;
    private final Long challengeId;
    private final String guildId;
    private final ProblemDifficulty difficulty;

    public DailyAssignmentRunner(DailyAssignmentService assignments,
                                 @Value("${daily-assign.challenge-id}") Long challengeId,
                                 @Value("${daily-assign.guild-id}") String guildId,
                                 @Value("${daily-assign.difficulty:EASY}") ProblemDifficulty difficulty) {
        this.assignments = assignments;
        this.challengeId = challengeId;
        this.guildId = guildId;
        this.difficulty = difficulty;
    }

    @Override
    public void run(ApplicationArguments args) {
        DailyAssignmentDto result = assignments.assignToday(challengeId, guildId, difficulty);
        log.info("Daily assignment: status={}, challengeId={}, date={}, problemId={}, slug={}, deadline={}.",
                result.isCreated() ? "CREATED" : "EXISTING", result.getChallengeId(), result.getAssignmentDate(),
                result.getProblemId(), result.getTitleSlug(), result.getDeadline());
    }
}
