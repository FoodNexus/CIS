-- Optional comma-separated interest tags for invitation rate (e.g. environment, technology)
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS interests TEXT NULL;
