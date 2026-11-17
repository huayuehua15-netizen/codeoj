package com.codeoj.common.persistence;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Shared SQL contract: UTC database clocks and a fresh token fence each attempt.
 * Business judged_at follows the +08:00 connection session, matching created_at and B's date queries.
 */
public interface SubmissionLeaseOperations {
    @Update("UPDATE submission SET status=1, judge_token=#{token}, " +
            "judge_lease_until=TIMESTAMPADD(SECOND, #{leaseSeconds}, UTC_TIMESTAMP(6)), " +
            "judge_attempts=judge_attempts+1 WHERE id=#{id} AND status=0")
    int markJudging(@Param("id") Long id, @Param("token") String token,
                    @Param("leaseSeconds") int leaseSeconds);

    @Update("UPDATE submission SET judge_lease_until=TIMESTAMPADD(SECOND, #{leaseSeconds}, UTC_TIMESTAMP(6)) " +
            "WHERE id=#{id} AND status=1 AND judge_token=#{token} AND judge_lease_until>UTC_TIMESTAMP(6)")
    int renewLease(@Param("id") Long id, @Param("token") String token,
                   @Param("leaseSeconds") int leaseSeconds);

    @Update("UPDATE submission SET status=0, judge_token=NULL, judge_lease_until=NULL, " +
            "next_dispatch_at=UTC_TIMESTAMP(6) WHERE id=#{id} AND status=1 AND judge_token=#{token}")
    int releaseClaim(@Param("id") Long id, @Param("token") String token);

    @Update("UPDATE submission SET status=0, judge_token=NULL, judge_lease_until=NULL, " +
            "next_dispatch_at=UTC_TIMESTAMP(6) WHERE status=1 " +
            "AND (judge_lease_until IS NULL OR judge_lease_until<=UTC_TIMESTAMP(6))")
    int requeueExpiredLeases();

    @Select("SELECT id FROM submission WHERE status=0 " +
            "AND (next_dispatch_at IS NULL OR next_dispatch_at<=UTC_TIMESTAMP(6)) ORDER BY next_dispatch_at, id LIMIT #{limit}")
    List<Long> findDispatchable(@Param("limit") int limit);

    @Update("UPDATE submission SET next_dispatch_at=TIMESTAMPADD(SECOND, #{delaySeconds}, UTC_TIMESTAMP(6)) " +
            "WHERE id=#{id} AND status=0 AND (next_dispatch_at IS NULL OR next_dispatch_at<=UTC_TIMESTAMP(6))")
    int reserveDispatch(@Param("id") Long id, @Param("delaySeconds") int delaySeconds);

    @Update("UPDATE submission SET status=#{status}, time_used_ms=#{timeUsedMs}, memory_used_kb=#{memoryUsedKb}, " +
            "error_info=#{errorInfo}, judged_at=NOW(), judge_token=NULL, judge_lease_until=NULL " +
            "WHERE id=#{id} AND status=1 AND judge_token=#{token} AND judge_lease_until>UTC_TIMESTAMP(6)")
    int updateFinalResult(@Param("id") Long id, @Param("token") String token,
                          @Param("status") int status, @Param("timeUsedMs") Integer timeUsedMs,
                          @Param("memoryUsedKb") Integer memoryUsedKb, @Param("errorInfo") String errorInfo);
}
