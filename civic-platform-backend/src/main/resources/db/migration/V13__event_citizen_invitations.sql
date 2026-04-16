-- Replace project-based volunteer invitations with event–citizen invitations
DROP TABLE IF EXISTS project_volunteer_matches;

CREATE TABLE IF NOT EXISTS event_citizen_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    citizen_id BIGINT NOT NULL,
    match_score DOUBLE NOT NULL,
    status ENUM('INVITED', 'ACCEPTED', 'DECLINED', 'NO_RESPONSE') NOT NULL DEFAULT 'INVITED',
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    invitation_token VARCHAR(255) NOT NULL,
    CONSTRAINT uk_eci_token UNIQUE (invitation_token),
    CONSTRAINT uk_eci_event_citizen UNIQUE (event_id, citizen_id),
    CONSTRAINT fk_eci_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT fk_eci_citizen FOREIGN KEY (citizen_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE INDEX idx_eci_event_id ON event_citizen_invitations(event_id);
CREATE INDEX idx_eci_citizen_id ON event_citizen_invitations(citizen_id);
CREATE INDEX idx_eci_invitation_token ON event_citizen_invitations(invitation_token);
