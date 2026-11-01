ALTER TABLE challenges
    RENAME COLUMN automation_difficulty TO difficulty;

ALTER TABLE challenges
    RENAME CONSTRAINT ck_challenges_automation_difficulty TO ck_challenges_difficulty;

ALTER TABLE challenges
    ADD COLUMN automation_frequency INTEGER NOT NULL DEFAULT 7;

ALTER TABLE challenges
    ADD CONSTRAINT ck_challenges_automation_frequency
    CHECK (automation_frequency >= 1 AND automation_frequency <= 7);

