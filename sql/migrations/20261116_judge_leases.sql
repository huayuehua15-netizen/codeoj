-- Upgrade an existing database once, with ALL old oj-server/oj-judge instances stopped.
-- Existing deployments must run this BEFORE starting the new binaries.
-- Fresh deployments already get these fields from schema.sql; do not run this again there.
ALTER TABLE submission
  ADD COLUMN judge_token VARCHAR(64) NULL,
  ADD COLUMN judge_lease_until DATETIME(6) NULL,
  ADD COLUMN judge_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN next_dispatch_at DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
  ADD INDEX idx_submission_dispatch (status, next_dispatch_at),
  ADD INDEX idx_submission_lease (status, judge_lease_until);

-- Legacy workers have no owner token. New recovery safely requeues them after old workers stop.
UPDATE submission SET next_dispatch_at=UTC_TIMESTAMP(6) WHERE status=0;
