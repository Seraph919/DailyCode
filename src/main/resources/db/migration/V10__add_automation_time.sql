ALTER TABLE challenges
    ADD COLUMN automation_time TIME NOT NULL DEFAULT '00:00:00',
    ADD CONSTRAINT ck_challenges_automation_time CHECK (
        automation_time < TIME '24:00:00'
        AND EXTRACT(SECOND FROM automation_time) = 0
    );
