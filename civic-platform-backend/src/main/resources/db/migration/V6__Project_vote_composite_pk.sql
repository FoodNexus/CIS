-- Replace surrogate id with composite PK (user_id, project_id) + voted_at
DROP TABLE IF EXISTS project_vote;

CREATE TABLE project_vote (
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, project_id),
    CONSTRAINT fk_project_vote_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_vote_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_vote_project_id ON project_vote(project_id);
CREATE INDEX idx_project_vote_user_id ON project_vote(user_id);
