ALTER TABLE challenges
    ADD COLUMN announcement_role_id VARCHAR(20),
    ADD CONSTRAINT ck_challenges_announcement_role CHECK (
        announcement_role_id IS NULL OR announcement_role_id ~ '^[0-9]{1,20}$'
    );
