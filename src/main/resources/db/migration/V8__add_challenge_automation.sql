ALTER TABLE challenges
    ADD COLUMN announcement_channel_id VARCHAR(20),
    ADD COLUMN automation_difficulty VARCHAR(10) NOT NULL DEFAULT 'EASY',
    ADD CONSTRAINT ck_challenges_announcement_channel CHECK (announcement_channel_id IS NULL OR announcement_channel_id ~ '^[0-9]{1,20}$'),
    ADD CONSTRAINT ck_challenges_automation_difficulty CHECK (automation_difficulty IN ('EASY','MEDIUM','HARD'));

ALTER TABLE daily_challenges
    ADD COLUMN announcement_attempted_at TIMESTAMPTZ,
    ADD COLUMN announcement_channel_id VARCHAR(20),
    ADD COLUMN announcement_message_id VARCHAR(20),
    ADD CONSTRAINT ck_daily_announcement_claim CHECK (
        (announcement_attempted_at IS NULL AND announcement_channel_id IS NULL AND announcement_message_id IS NULL)
        OR (announcement_attempted_at IS NOT NULL AND announcement_channel_id IS NOT NULL)
    );

CREATE INDEX ix_challenges_automated ON challenges (id) WHERE announcement_channel_id IS NOT NULL;
