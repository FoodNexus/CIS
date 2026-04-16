-- Volunteer–project matching invitations and responses
CREATE TABLE IF NOT EXISTS project_volunteer_matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    volunteer_id BIGINT NOT NULL,
    match_score DOUBLE NOT NULL,
    status ENUM('INVITED', 'ACCEPTED', 'DECLINED', 'NO_RESPONSE') NOT NULL DEFAULT 'INVITED',
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    invitation_token VARCHAR(255) NOT NULL,
    CONSTRAINT uk_pvm_token UNIQUE (invitation_token),
    CONSTRAINT uk_pvm_project_volunteer UNIQUE (project_id, volunteer_id),
    CONSTRAINT fk_pvm_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_pvm_volunteer FOREIGN KEY (volunteer_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE INDEX idx_pvm_project_id ON project_volunteer_matches(project_id);
CREATE INDEX idx_pvm_volunteer_id ON project_volunteer_matches(volunteer_id);
CREATE INDEX idx_pvm_invitation_token ON project_volunteer_matches(invitation_token);
