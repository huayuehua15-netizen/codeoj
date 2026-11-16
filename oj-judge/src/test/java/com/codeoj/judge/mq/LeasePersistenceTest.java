package com.codeoj.judge.mq;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.codeoj.common.entity.Submission;
import com.codeoj.common.enums.SubmissionStatus;
import com.codeoj.judge.core.*;
import com.codeoj.judge.core.result.ResultWriter;
import com.codeoj.judge.mapper.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class LeasePersistenceTest {
    JdbcTemplate jdbc;
    SubmissionMapper mapper;
    ResultWriter writer;
    public static LocalDateTime utcTimestamp() { return LocalDateTime.now(ZoneOffset.UTC); }
    public static LocalDateTime utcTimestamp(int precision) { return utcTimestamp(); }

    @BeforeEach void setup() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=SET TIME ZONE '+08:00'");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE ALIAS UTC_TIMESTAMP FOR 'com.codeoj.judge.mq.LeasePersistenceTest.utcTimestamp'");
        jdbc.execute("CREATE TABLE submission(id BIGINT PRIMARY KEY, user_id BIGINT, problem_id BIGINT, language VARCHAR, code VARCHAR, status INT, time_used_ms INT, memory_used_kb INT, error_info VARCHAR, judged_at TIMESTAMP, created_at TIMESTAMP, judge_token VARCHAR, judge_lease_until TIMESTAMP(6), judge_attempts INT DEFAULT 0, next_dispatch_at TIMESTAMP(6) DEFAULT (UTC_TIMESTAMP(6)))");
        jdbc.execute("CREATE TABLE judge_case(id BIGINT AUTO_INCREMENT PRIMARY KEY, submission_id BIGINT, case_index INT, status INT CHECK(status=2), time_used_ms INT, memory_used_kb INT)");
        jdbc.execute("CREATE TABLE problem(id BIGINT PRIMARY KEY, submit_count INT, accepted_count INT)");
        jdbc.update("INSERT INTO problem VALUES(3,0,0)");
        jdbc.update("INSERT INTO submission(id,user_id,problem_id,status) VALUES(7,2,3,0)");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new org.mybatis.spring.transaction.SpringManagedTransactionFactory(), ds));
        config.addMapper(SubmissionMapper.class);
        config.addMapper(JudgeCaseMapper.class);
        config.addMapper(ProblemMapper.class);
        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(config);
        SqlSessionTemplate session = new SqlSessionTemplate(factory);
        mapper = session.getMapper(SubmissionMapper.class);
        ResultWriter target = new ResultWriter(mapper, session.getMapper(JudgeCaseMapper.class), session.getMapper(ProblemMapper.class));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds), new AnnotationTransactionAttributeSource()));
        writer = (ResultWriter) proxy.getProxy();
    }
    @Test void liveLeaseIsNotStolenAndExpiredOwnerCannotCommitToNewAttempt() {
        assertEquals(1, mapper.markJudging(7L, "old", 120));
        assertEquals(0, mapper.markJudging(7L, "other", 120));
        assertEquals(0, mapper.requeueExpiredLeases()); // expansion leaves a healthy worker alone
        jdbc.update("UPDATE submission SET judge_lease_until=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP(6)) WHERE id=7");
        assertEquals(0, mapper.renewLease(7L, "old", 120)); // expiry cannot be resurrected
        assertEquals(1, mapper.requeueExpiredLeases());
        assertEquals(1, mapper.markJudging(7L, "new", 120));
        assertEquals(0, mapper.releaseClaim(7L, "old"));
        JudgeResult accepted = result(SubmissionStatus.ACCEPTED);
        assertFalse(writer.save(7, "old", accepted));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM judge_case", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT submit_count FROM problem WHERE id=3", Integer.class));
        assertTrue(writer.save(7, "new", accepted));
        assertFalse(writer.save(7, "new", accepted));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM judge_case", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT accepted_count FROM problem WHERE id=3", Integer.class));
    }
    @Test void detailInsertFailureRollsBackTerminalStatusAndStatisticsTogether() {
        mapper.markJudging(7L, "owner", 120);
        assertThrows(RuntimeException.class, () -> writer.save(7, "owner", result(SubmissionStatus.WRONG_ANSWER)));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM submission WHERE id=7", Integer.class));
        assertEquals("owner", jdbc.queryForObject("SELECT judge_token FROM submission WHERE id=7", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT submit_count FROM problem WHERE id=3", Integer.class));
    }
    @Test void dispatchReservationExpiresAndOtherScannersCannotPublishDuringWindow() {
        assertTrue(mapper.findDispatchable(200).contains(7L));
        assertEquals(1, mapper.reserveDispatch(7L,120));
        assertEquals(0, mapper.reserveDispatch(7L,120));
        assertTrue(mapper.findDispatchable(200).isEmpty());
        jdbc.update("UPDATE submission SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP(6)) WHERE id=7");
        assertTrue(mapper.findDispatchable(200).contains(7L));
    }
    @Test void repeatedAcSubmissionsCountOneDistinctSolvedProblem() {
        jdbc.update("UPDATE submission SET status=2 WHERE id=7");
        jdbc.update("INSERT INTO submission(id,user_id,problem_id,status) VALUES(8,2,3,2)");
        assertEquals(1, mapper.countAcceptedProblems(2L));
        jdbc.update("INSERT INTO submission(id,user_id,problem_id,status) VALUES(9,2,4,2)");
        assertEquals(2, mapper.countAcceptedProblems(2L));
    }
    @Test void shanghaiSessionKeepsDispatchAndLeaseInUtcButBusinessResultInShanghai() {
        // A fresh row is immediately dispatchable even though NOW() is eight hours ahead of UTC.
        assertTrue(mapper.findDispatchable(200).contains(7L));
        assertEquals(1, mapper.reserveDispatch(7L, 120));
        LocalDateTime before = utcTimestamp();
        assertEquals(1, mapper.markJudging(7L, "clock-owner", 120));
        LocalDateTime lease = jdbc.queryForObject("SELECT judge_lease_until FROM submission WHERE id=7",
                LocalDateTime.class);
        assertNotNull(lease);
        long remaining = Duration.between(before, lease).getSeconds();
        assertTrue(remaining >= 119 && remaining <= 121, "lease duration must remain 120 UTC seconds");
        assertEquals(1, mapper.renewLease(7L, "clock-owner", 120));
        assertTrue(writer.save(7, "clock-owner", result(SubmissionStatus.ACCEPTED)));
        LocalDateTime judged = jdbc.queryForObject("SELECT judged_at FROM submission WHERE id=7", LocalDateTime.class);
        assertNotNull(judged);
        long businessOffset = Duration.between(before, judged).getSeconds();
        assertTrue(businessOffset >= 28799 && businessOffset <= 28801,
                "judged_at must match Shanghai business dates, not UTC lease dates");
    }
    private JudgeResult result(SubmissionStatus status) {
        return JudgeResult.builder().status(status).cases(List.of(CaseResult.builder()
                .index(1).status(status).timeUsedMs(1).memoryUsedKb(1).build())).build();
    }
}
