-- Track project creator for integrity (e.g. self-vote prevention)
ALTER TABLE project ADD COLUMN created_by_id BIGINT NULL;

ALTER TABLE project
  ADD CONSTRAINT fk_project_created_by
  FOREIGN KEY (created_by_id) REFERENCES `user`(id);

CREATE INDEX idx_project_created_by_id ON project(created_by_id);
