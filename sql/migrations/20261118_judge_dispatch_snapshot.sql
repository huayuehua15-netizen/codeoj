-- Existing databases only, after lease/UTC and legacy business schema migrations as applicable.
-- Back up first and stop all old oj-server/oj-judge instances. Run once before deploying both.
ALTER TABLE submission
  ADD COLUMN dispatch_token CHAR(36) DEFAULT NULL,
  ADD COLUMN dispatched_at DATETIME(6) DEFAULT NULL,
  ADD COLUMN judge_snapshot_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  ADD COLUMN total_cases INT DEFAULT NULL,
  DROP INDEX idx_submission_dispatch,
  ADD INDEX idx_submission_dispatch (status, dispatched_at, next_dispatch_at);

CREATE TABLE IF NOT EXISTS judge_problem_snapshot (
  snapshot_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  payload LONGBLOB NOT NULL,
  PRIMARY KEY (snapshot_key)
) ENGINE=InnoDB;
