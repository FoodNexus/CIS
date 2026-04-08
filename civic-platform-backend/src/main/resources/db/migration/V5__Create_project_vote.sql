CREATE TABLE IF NOT EXISTS project_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    UNIQUE KEY uk_project_vote_user_project (user_id, project_id),
    CONSTRAINT fk_project_vote_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_vote_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_vote_project_id ON project_vote(project_id);
CREATE INDEX IF NOT EXISTS idx_project_vote_user_id ON project_vote(user_id);
