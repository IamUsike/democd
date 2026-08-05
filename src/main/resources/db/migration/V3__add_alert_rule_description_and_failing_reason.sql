ALTER TABLE alerts
    ADD COLUMN rule_description VARCHAR(500) NULL;

ALTER TABLE alerts
    ADD COLUMN failing_reason VARCHAR(1000) NULL;

