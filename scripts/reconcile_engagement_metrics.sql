-- Reconcile denormalized counters and lifecycle-derived user state
-- Run against civic_platform database.

START TRANSACTION;

-- 1) Participation status consistency with event lifecycle.
-- If event not completed, no participation should remain COMPLETED yet.
UPDATE event_participant ep
JOIN event e ON e.id = ep.event_id
SET ep.status = CASE
        WHEN ep.checked_in_at IS NOT NULL THEN 'CHECKED_IN'
        ELSE 'REGISTERED'
    END,
    ep.completed_at = NULL
WHERE e.status <> 'COMPLETED'
  AND ep.status = 'COMPLETED';

-- When event completes, all active participants become completed.
UPDATE event_participant ep
JOIN event e ON e.id = ep.event_id
SET ep.status = 'COMPLETED',
    ep.completed_at = COALESCE(ep.completed_at, NOW())
WHERE e.status = 'COMPLETED'
  AND ep.status IN ('REGISTERED', 'CHECKED_IN');

-- Cancelled events cannot keep active participants.
UPDATE event_participant ep
JOIN event e ON e.id = ep.event_id
SET ep.status = 'CANCELLED',
    ep.completed_at = NULL
WHERE e.status = 'CANCELLED'
  AND ep.status IN ('REGISTERED', 'CHECKED_IN');

-- 2) Event participant counter from real active rows only.
UPDATE event e
LEFT JOIN (
    SELECT event_id, COUNT(*) AS active_cnt
    FROM event_participant
    WHERE status IN ('REGISTERED', 'CHECKED_IN')
    GROUP BY event_id
) x ON x.event_id = e.id
SET e.current_participants = COALESCE(x.active_cnt, 0);

-- 3) Post likes count from like table.
UPDATE post p
LEFT JOIN (
    SELECT post_id, COUNT(*) AS like_cnt
    FROM like_entity
    GROUP BY post_id
) l ON l.post_id = p.id
SET p.likes_count = COALESCE(l.like_cnt, 0);

-- 4) Project funding/votes from action tables and status normalization.
UPDATE project p
LEFT JOIN (
    SELECT project_id, COALESCE(SUM(amount), 0) AS amount_sum
    FROM project_funding
    GROUP BY project_id
) f ON f.project_id = p.id
LEFT JOIN (
    SELECT project_id, COUNT(*) AS vote_cnt
    FROM project_vote
    GROUP BY project_id
) v ON v.project_id = p.id
SET p.current_amount = COALESCE(f.amount_sum, 0),
    p.vote_count = COALESCE(v.vote_cnt, 0),
    p.status = CASE
        WHEN p.completion_date IS NOT NULL THEN 'COMPLETED'
        WHEN COALESCE(f.amount_sum, 0) >= p.goal_amount THEN 'FULLY_FUNDED'
        ELSE 'SUBMITTED'
    END;

-- 5) User points/badge and lifecycle role from real participation.
DROP TEMPORARY TABLE IF EXISTS tmp_user_engagement_rollup;
CREATE TEMPORARY TABLE tmp_user_engagement_rollup AS
SELECT
    u.id AS user_id,
    COALESCE(done.completed_count, 0) AS completed_count,
    CASE
        WHEN active.user_id IS NULL THEN 0
        ELSE 1
    END AS has_active_event
FROM user u
LEFT JOIN (
    SELECT ep.user_id, COUNT(*) AS completed_count
    FROM event_participant ep
    JOIN event e ON e.id = ep.event_id
    WHERE ep.status = 'COMPLETED' AND e.status = 'COMPLETED'
    GROUP BY ep.user_id
) done ON done.user_id = u.id
LEFT JOIN (
    SELECT DISTINCT ep.user_id
    FROM event_participant ep
    JOIN event e ON e.id = ep.event_id
    WHERE ep.status IN ('REGISTERED', 'CHECKED_IN')
      AND e.status IN ('UPCOMING', 'ONGOING')
) active ON active.user_id = u.id;

UPDATE user u
JOIN tmp_user_engagement_rollup r ON r.user_id = u.id
SET u.points = r.completed_count,
    u.badge = CASE
        WHEN r.completed_count >= 8 THEN 'PLATINUM'
        WHEN r.completed_count >= 5 THEN 'GOLD'
        WHEN r.completed_count >= 3 THEN 'SILVER'
        WHEN r.completed_count >= 1 THEN 'BRONZE'
        ELSE 'NONE'
    END,
    u.awarded_date = CASE
        WHEN r.completed_count = 0 THEN NULL
        ELSE COALESCE(u.awarded_date, CURDATE())
    END
WHERE u.is_admin = b'0';

-- UserType lifecycle:
-- - DONOR unchanged
-- - AMBASSADOR if >= 5 completed events
-- - PARTICIPANT while active participation exists
-- - otherwise CITIZEN
UPDATE user u
JOIN tmp_user_engagement_rollup r ON r.user_id = u.id
SET u.user_type = CASE
        WHEN u.user_type = 'DONOR' THEN 'DONOR'
        WHEN r.completed_count >= 5 THEN 'AMBASSADOR'
        WHEN r.has_active_event = 1 THEN 'PARTICIPANT'
        ELSE 'CITIZEN'
    END
WHERE u.is_admin = b'0'
  AND (u.user_type IS NULL OR u.user_type <> 'DONOR');

DROP TEMPORARY TABLE IF EXISTS tmp_user_engagement_rollup;

COMMIT;
