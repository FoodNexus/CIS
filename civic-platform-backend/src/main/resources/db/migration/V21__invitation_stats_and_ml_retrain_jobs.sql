CREATE TABLE IF NOT EXISTS event_matching_run_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    run_id VARCHAR(64) NOT NULL UNIQUE,
    candidate_count INT NOT NULL,
    direct_invite_count INT NOT NULL,
    nurture_count INT NOT NULL,
    average_composite_rate DOUBLE NOT NULL,
    max_composite_rate DOUBLE NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_emrs_event FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE INDEX idx_emrs_event_created ON event_matching_run_stats(event_id, created_at DESC);

CREATE TABLE IF NOT EXISTS event_invitation_match_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    citizen_id BIGINT NOT NULL,
    citizen_name VARCHAR(255) NOT NULL,
    citizen_user_type VARCHAR(32),
    composite_rate DOUBLE NOT NULL,
    raw_score DOUBLE NOT NULL,
    invitation_tier VARCHAR(40),
    priority_followup BOOLEAN NOT NULL DEFAULT FALSE,
    selected_for_direct_invite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_eimh_event FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE INDEX idx_eimh_run_id ON event_invitation_match_history(run_id);
CREATE INDEX idx_eimh_event_created ON event_invitation_match_history(event_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ml_retrain_job_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT
);

CREATE INDEX idx_mrjr_started_at ON ml_retrain_job_runs(started_at DESC);
