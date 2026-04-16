-- Rate-driven invitation metadata (composite 0–100, tier, feature snapshot JSON)
ALTER TABLE event_citizen_invitations
    ADD COLUMN composite_rate DOUBLE NULL COMMENT '0-100 normalized match rate' AFTER match_score,
    ADD COLUMN invitation_tier VARCHAR(40) NULL AFTER composite_rate,
    ADD COLUMN priority_followup BOOLEAN NOT NULL DEFAULT FALSE AFTER invitation_tier,
    ADD COLUMN feature_breakdown_json TEXT NULL AFTER priority_followup;

-- Backfill legacy rows: derive composite from match_score (legacy scale ~0–125)
UPDATE event_citizen_invitations
SET composite_rate = LEAST(100.0, GREATEST(0.0, (match_score / 125.0) * 100.0)),
    invitation_tier = CASE
        WHEN (match_score / 125.0) * 100.0 >= 72 THEN 'PRIORITY_IMMEDIATE'
        WHEN (match_score / 125.0) * 100.0 >= 45 THEN 'STANDARD_INVITE'
        ELSE 'STANDARD_INVITE'
    END,
    priority_followup = (match_score / 125.0) * 100.0 >= 72
WHERE composite_rate IS NULL;
