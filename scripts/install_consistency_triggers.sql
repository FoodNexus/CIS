-- Install DB-level consistency triggers/procedures.
-- These guard denormalized counters and engagement-derived fields
-- even if direct SQL writes bypass application services.
--
-- Target: MariaDB / civic_platform

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_recalc_post_likes $$
CREATE PROCEDURE sp_recalc_post_likes(IN p_post_id BIGINT)
BEGIN
    UPDATE post p
    SET p.likes_count = (
        SELECT COUNT(*)
        FROM like_entity l
        WHERE l.post_id = p.id
    )
    WHERE p.id = p_post_id;
END $$

DROP PROCEDURE IF EXISTS sp_recalc_project_metrics $$
CREATE PROCEDURE sp_recalc_project_metrics(IN p_project_id BIGINT)
BEGIN
    DECLARE v_goal DECIMAL(15,2);
    DECLARE v_completion_date DATE;
    DECLARE v_sum DECIMAL(15,2);
    DECLARE v_votes INT;

    SELECT goal_amount, completion_date
    INTO v_goal, v_completion_date
    FROM project
    WHERE id = p_project_id;

    SELECT COALESCE(SUM(amount), 0)
    INTO v_sum
    FROM project_funding
    WHERE project_id = p_project_id;

    SELECT COUNT(*)
    INTO v_votes
    FROM project_vote
    WHERE project_id = p_project_id;

    UPDATE project
    SET current_amount = COALESCE(v_sum, 0),
        vote_count = COALESCE(v_votes, 0),
        status = CASE
            WHEN v_completion_date IS NOT NULL THEN 'COMPLETED'
            WHEN COALESCE(v_sum, 0) >= COALESCE(v_goal, 0) AND COALESCE(v_goal, 0) > 0 THEN 'FULLY_FUNDED'
            ELSE 'SUBMITTED'
        END
    WHERE id = p_project_id;
END $$

DROP PROCEDURE IF EXISTS sp_recalc_event_participants $$
CREATE PROCEDURE sp_recalc_event_participants(IN p_event_id BIGINT)
BEGIN
    UPDATE event e
    SET e.current_participants = (
        SELECT COUNT(*)
        FROM event_participant ep
        WHERE ep.event_id = e.id
          AND ep.status IN ('REGISTERED', 'CHECKED_IN')
    )
    WHERE e.id = p_event_id;
END $$

DROP PROCEDURE IF EXISTS sp_recalc_user_engagement $$
CREATE PROCEDURE sp_recalc_user_engagement(IN p_user_id BIGINT)
BEGIN
    DECLARE v_completed BIGINT DEFAULT 0;
    DECLARE v_has_active INT DEFAULT 0;
    DECLARE v_is_admin BIT DEFAULT b'0';
    DECLARE v_type VARCHAR(50);
    DECLARE v_badge VARCHAR(20);
    DECLARE v_awarded DATE;

    SELECT is_admin, user_type
    INTO v_is_admin, v_type
    FROM user
    WHERE id = p_user_id;

    IF v_is_admin <> b'1' THEN

        SELECT COUNT(*)
        INTO v_completed
        FROM event_participant ep
        JOIN event e ON e.id = ep.event_id
        WHERE ep.user_id = p_user_id
          AND ep.status = 'COMPLETED'
          AND e.status = 'COMPLETED';

        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
        INTO v_has_active
        FROM event_participant ep
        JOIN event e ON e.id = ep.event_id
        WHERE ep.user_id = p_user_id
          AND ep.status IN ('REGISTERED', 'CHECKED_IN')
          AND e.status IN ('UPCOMING', 'ONGOING');

        SET v_badge = CASE
            WHEN v_completed >= 8 THEN 'PLATINUM'
            WHEN v_completed >= 5 THEN 'GOLD'
            WHEN v_completed >= 3 THEN 'SILVER'
            WHEN v_completed >= 1 THEN 'BRONZE'
            ELSE 'NONE'
        END;

        SET v_awarded = CASE
            WHEN v_completed = 0 THEN NULL
            ELSE CURDATE()
        END;

        UPDATE user u
        SET u.points = v_completed,
            u.badge = v_badge,
            u.awarded_date = CASE
                WHEN v_awarded IS NULL THEN NULL
                WHEN u.awarded_date IS NULL THEN v_awarded
                ELSE u.awarded_date
            END,
            u.user_type = CASE
                WHEN u.user_type = 'DONOR' THEN 'DONOR'
                WHEN v_completed >= 5 THEN 'AMBASSADOR'
                WHEN v_has_active = 1 THEN 'PARTICIPANT'
                ELSE 'CITIZEN'
            END
        WHERE u.id = p_user_id
          AND u.is_admin = b'0';
    END IF;
END $$

DROP PROCEDURE IF EXISTS sp_recalc_users_for_event $$
CREATE PROCEDURE sp_recalc_users_for_event(IN p_event_id BIGINT)
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE cur CURSOR FOR
        SELECT DISTINCT ep.user_id
        FROM event_participant ep
        WHERE ep.event_id = p_event_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    user_loop: LOOP
        FETCH cur INTO v_user_id;
        IF done = 1 THEN
            LEAVE user_loop;
        END IF;
        CALL sp_recalc_user_engagement(v_user_id);
    END LOOP;
    CLOSE cur;
END $$

DROP TRIGGER IF EXISTS trg_like_entity_ai $$
CREATE TRIGGER trg_like_entity_ai
AFTER INSERT ON like_entity
FOR EACH ROW
BEGIN
    CALL sp_recalc_post_likes(NEW.post_id);
END $$

DROP TRIGGER IF EXISTS trg_like_entity_ad $$
CREATE TRIGGER trg_like_entity_ad
AFTER DELETE ON like_entity
FOR EACH ROW
BEGIN
    CALL sp_recalc_post_likes(OLD.post_id);
END $$

DROP TRIGGER IF EXISTS trg_like_entity_au $$
CREATE TRIGGER trg_like_entity_au
AFTER UPDATE ON like_entity
FOR EACH ROW
BEGIN
    CALL sp_recalc_post_likes(OLD.post_id);
    IF NEW.post_id <> OLD.post_id THEN
        CALL sp_recalc_post_likes(NEW.post_id);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_post_bi_enforce_likes $$
CREATE TRIGGER trg_post_bi_enforce_likes
BEFORE INSERT ON post
FOR EACH ROW
BEGIN
    SET NEW.likes_count = 0;
END $$

DROP TRIGGER IF EXISTS trg_post_bu_enforce_likes $$
CREATE TRIGGER trg_post_bu_enforce_likes
BEFORE UPDATE ON post
FOR EACH ROW
BEGIN
    SET NEW.likes_count = (
        SELECT COUNT(*)
        FROM like_entity l
        WHERE l.post_id = OLD.id
    );
END $$

DROP TRIGGER IF EXISTS trg_project_bi_enforce_rollups $$
CREATE TRIGGER trg_project_bi_enforce_rollups
BEFORE INSERT ON project
FOR EACH ROW
BEGIN
    SET NEW.current_amount = 0;
    SET NEW.vote_count = 0;
    IF NEW.completion_date IS NOT NULL THEN
        SET NEW.status = 'COMPLETED';
    ELSE
        SET NEW.status = 'SUBMITTED';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_project_bu_enforce_rollups $$
CREATE TRIGGER trg_project_bu_enforce_rollups
BEFORE UPDATE ON project
FOR EACH ROW
BEGIN
    DECLARE v_sum DECIMAL(15,2);
    DECLARE v_votes INT;

    SELECT COALESCE(SUM(amount), 0)
    INTO v_sum
    FROM project_funding
    WHERE project_id = OLD.id;

    SELECT COUNT(*)
    INTO v_votes
    FROM project_vote
    WHERE project_id = OLD.id;

    SET NEW.current_amount = v_sum;
    SET NEW.vote_count = v_votes;
    SET NEW.status = CASE
        WHEN NEW.completion_date IS NOT NULL THEN 'COMPLETED'
        WHEN v_sum >= COALESCE(NEW.goal_amount, 0) AND COALESCE(NEW.goal_amount, 0) > 0 THEN 'FULLY_FUNDED'
        ELSE 'SUBMITTED'
    END;
END $$

DROP TRIGGER IF EXISTS trg_event_bi_enforce_participants $$
CREATE TRIGGER trg_event_bi_enforce_participants
BEFORE INSERT ON event
FOR EACH ROW
BEGIN
    SET NEW.current_participants = 0;
END $$

DROP TRIGGER IF EXISTS trg_event_bu_enforce_participants $$
CREATE TRIGGER trg_event_bu_enforce_participants
BEFORE UPDATE ON event
FOR EACH ROW
BEGIN
    SET NEW.current_participants = (
        SELECT COUNT(*)
        FROM event_participant ep
        WHERE ep.event_id = OLD.id
          AND ep.status IN ('REGISTERED', 'CHECKED_IN')
    );
END $$

DROP TRIGGER IF EXISTS trg_project_funding_ai $$
CREATE TRIGGER trg_project_funding_ai
AFTER INSERT ON project_funding
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(NEW.project_id);
    CALL sp_recalc_user_engagement(NEW.user_id);
END $$

DROP TRIGGER IF EXISTS trg_project_funding_ad $$
CREATE TRIGGER trg_project_funding_ad
AFTER DELETE ON project_funding
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(OLD.project_id);
    CALL sp_recalc_user_engagement(OLD.user_id);
END $$

DROP TRIGGER IF EXISTS trg_project_funding_au $$
CREATE TRIGGER trg_project_funding_au
AFTER UPDATE ON project_funding
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(OLD.project_id);
    CALL sp_recalc_user_engagement(OLD.user_id);
    IF NEW.project_id <> OLD.project_id THEN
        CALL sp_recalc_project_metrics(NEW.project_id);
    END IF;
    IF NEW.user_id <> OLD.user_id THEN
        CALL sp_recalc_user_engagement(NEW.user_id);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_project_vote_ai $$
CREATE TRIGGER trg_project_vote_ai
AFTER INSERT ON project_vote
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(NEW.project_id);
END $$

DROP TRIGGER IF EXISTS trg_project_vote_ad $$
CREATE TRIGGER trg_project_vote_ad
AFTER DELETE ON project_vote
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(OLD.project_id);
END $$

DROP TRIGGER IF EXISTS trg_project_vote_au $$
CREATE TRIGGER trg_project_vote_au
AFTER UPDATE ON project_vote
FOR EACH ROW
BEGIN
    CALL sp_recalc_project_metrics(OLD.project_id);
    IF NEW.project_id <> OLD.project_id THEN
        CALL sp_recalc_project_metrics(NEW.project_id);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_event_participant_ai $$
CREATE TRIGGER trg_event_participant_ai
AFTER INSERT ON event_participant
FOR EACH ROW
BEGIN
    CALL sp_recalc_event_participants(NEW.event_id);
    CALL sp_recalc_user_engagement(NEW.user_id);
END $$

DROP TRIGGER IF EXISTS trg_event_participant_ad $$
CREATE TRIGGER trg_event_participant_ad
AFTER DELETE ON event_participant
FOR EACH ROW
BEGIN
    CALL sp_recalc_event_participants(OLD.event_id);
    CALL sp_recalc_user_engagement(OLD.user_id);
END $$

DROP TRIGGER IF EXISTS trg_event_participant_au $$
CREATE TRIGGER trg_event_participant_au
AFTER UPDATE ON event_participant
FOR EACH ROW
BEGIN
    CALL sp_recalc_event_participants(OLD.event_id);
    CALL sp_recalc_user_engagement(OLD.user_id);
    IF NEW.event_id <> OLD.event_id THEN
        CALL sp_recalc_event_participants(NEW.event_id);
    END IF;
    IF NEW.user_id <> OLD.user_id THEN
        CALL sp_recalc_user_engagement(NEW.user_id);
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_event_au $$
CREATE TRIGGER trg_event_au
AFTER UPDATE ON event
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        CALL sp_recalc_event_participants(NEW.id);
        CALL sp_recalc_users_for_event(NEW.id);
    END IF;
END $$

DELIMITER ;

