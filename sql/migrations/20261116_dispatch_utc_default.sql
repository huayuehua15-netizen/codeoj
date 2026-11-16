-- Only for databases that already received an earlier version of the lease migration.
-- Stop old workers first. Fresh schema/current lease migration already has this default.
ALTER TABLE submission
  MODIFY COLUMN next_dispatch_at DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6));

-- No delayed-task feature exists; a normal reservation lasts 120 seconds.
-- Repair legacy +08:00 defaults without disturbing normal dispatch reservations.
UPDATE submission SET next_dispatch_at=UTC_TIMESTAMP(6)
WHERE status=0 AND next_dispatch_at>TIMESTAMPADD(MINUTE,10,UTC_TIMESTAMP(6));
