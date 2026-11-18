package com.codeoj.server.service;

import com.codeoj.common.entity.*;
import com.codeoj.server.dto.SubmitDTO;
import com.codeoj.server.mapper.*;
import com.codeoj.server.mq.JudgeDispatcher;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.impl.SubmissionServiceImpl;
import com.codeoj.server.ws.PushEventPublisher;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SubmissionDispatchTest {
    SubmissionMapper submissions = mock(SubmissionMapper.class);
    ProblemMapper problems = mock(ProblemMapper.class);
    TestCaseMapper testCases = mock(TestCaseMapper.class);
    JudgeDispatcher dispatcher = mock(JudgeDispatcher.class);
    PushEventPublisher pushes = mock(PushEventPublisher.class);
    RecordingTransactionManager transactions = new RecordingTransactionManager();
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactions);
    SubmissionServiceImpl service;
    SubmitDTO dto;

    @BeforeEach void setup() {
        IdempotentLockService lock = mock(IdempotentLockService.class);
        when(lock.tryLock(anyString(), anyInt())).thenReturn(true);
        Problem problem = new Problem();
        problem.setEnabled(true);
        problem.setLanguages(List.of("c"));
        problem.setTimeLimitMs(1000);
        problem.setMemoryLimitMb(256);
        when(problems.selectById(3L)).thenReturn(problem);
        when(submissions.insert(any(Submission.class))).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                    "createQueued must execute inside B's TransactionTemplate");
            call.<Submission>getArgument(0).setId(7L);
            return 1;
        });
        service = new SubmissionServiceImpl(submissions, problems, mock(JudgeCaseMapper.class), testCases,
                mock(RateLimiter.class), lock, dispatcher, pushes, transactionTemplate);
        ReflectionTestUtils.setField(service, "maxCodeBytes",65536);
        ReflectionTestUtils.setField(service, "submitPerMinute",6);
        dto = new SubmitDTO();
        dto.setProblemId(3L);
        dto.setLanguage("c");
        dto.setCode("int main(){}");
    }
    @Test void acceptanceStoresAnImmutableOrderedSnapshotWithTheSubmission() {
        TestCase first = new TestCase(); first.setInput("一\r\n"); first.setExpectedOutput("first");
        TestCase second = new TestCase(); second.setInput(""); second.setExpectedOutput("second");
        when(testCases.listJudgeCases(eq(3L), anyInt())).thenReturn(List.of(first, second));
        var payload = ArgumentCaptor.forClass(byte[].class);
        var key = ArgumentCaptor.forClass(String.class);
        var row = ArgumentCaptor.forClass(Submission.class);
        service.submit(new LoginUser(2L, 0, "jti"), dto);
        var order = inOrder(submissions);
        order.verify(submissions).insertJudgeSnapshot(key.capture(), payload.capture());
        order.verify(submissions).insert(row.capture());
        first.setExpectedOutput("edited");
        var snapshot = com.codeoj.common.model.JudgeSnapshot.decode(
                new String(payload.getValue(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(List.of("first", "second"), snapshot.cases().stream()
                .map(com.codeoj.common.model.JudgeSnapshot.OneCase::expectedOutput).toList());
        assertEquals("一\r\n", snapshot.cases().get(0).input());
        assertEquals(2, row.getValue().getTotalCases());
        assertEquals(key.getValue(), row.getValue().getJudgeSnapshotKey());
    }

    @Test void actualTransactionCommitsBeforeQueuedPushThenMqAndReturnsNumericStatus() {
        doAnswer(call -> {
            assertEquals(1, transactions.commits);
            return null;
        }).when(pushes).publishQueued(2L,7L);
        var response = service.submit(new LoginUser(2L,0,"jti"), dto);
        assertEquals(7L, response.getSubmissionId());
        assertEquals(0, response.getStatus());
        var order = inOrder(submissions,pushes,dispatcher);
        order.verify(submissions).insert(any(Submission.class));
        order.verify(pushes).publishQueued(2L,7L);
        order.verify(dispatcher).publish(7L);
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
    }

    @Test void joiningOuterTransactionStillDefersPushAndMqUntilItsCommit() {
        transactionTemplate.execute(status -> {
            service.submit(new LoginUser(2L,0,"jti"), dto);
            verifyNoInteractions(pushes,dispatcher);
            assertEquals(0, transactions.commits);
            return null;
        });
        assertEquals(1, transactions.commits);
        verify(pushes).publishQueued(2L,7L);
        verify(dispatcher).publish(7L);
    }

    @Test void outerRollbackDoesNotPublishAcceptedButUncommittedSubmission() {
        assertThrows(IllegalStateException.class, () -> transactionTemplate.execute(status -> {
            service.submit(new LoginUser(2L,0,"jti"), dto);
            throw new IllegalStateException("outer operation failed");
        }));
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        verifyNoInteractions(pushes,dispatcher);
    }

    @Test void failedInsertRollsBackWithoutPushOrDispatch() {
        doThrow(new IllegalStateException("DB")).when(submissions).insert(any(Submission.class));
        assertThrows(IllegalStateException.class, () -> service.submit(new LoginUser(2L,0,"jti"), dto));
        assertEquals(1, transactions.rollbacks);
        verifyNoInteractions(pushes,dispatcher);
    }

    @Test void postCommitSideEffectFailuresDoNotTurnAcceptedSubmissionIntoHttpFailure() {
        doThrow(new IllegalStateException("WS")).when(pushes).publishQueued(2L,7L);
        doThrow(new IllegalStateException("dispatch transaction")).when(dispatcher).publish(7L);
        assertDoesNotThrow(() -> service.submit(new LoginUser(2L,0,"jti"), dto));
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
        verify(dispatcher).publish(7L);
    }

    @Test void createdAtUsesShanghaiBusinessTimeAndInitialAttemptsAreZero() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusSeconds(1);
        service.submit(new LoginUser(2L,0,"jti"), dto);
        LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusSeconds(1);
        ArgumentCaptor<Submission> row = ArgumentCaptor.forClass(Submission.class);
        verify(submissions).insert(row.capture());
        assertFalse(row.getValue().getCreatedAt().isBefore(before));
        assertFalse(row.getValue().getCreatedAt().isAfter(after));
        assertEquals(0,row.getValue().getJudgeAttempts());
    }

    /** Real Spring commit/rollback/synchronization lifecycle without an external database. */
    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private final ThreadLocal<State> current = new ThreadLocal<>();
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() {
            State state = current.get();
            return state == null ? new State() : state;
        }
        @Override protected boolean isExistingTransaction(Object transaction) {
            return ((State) transaction).active;
        }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            State state = (State) transaction;
            state.active = true;
            current.set(state);
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            ((State) status.getTransaction()).rollbackOnly = true;
        }
        @Override protected void doCleanupAfterCompletion(Object transaction) {
            ((State) transaction).active = false;
            current.remove();
        }
        private static final class State implements SmartTransactionObject {
            boolean active;
            boolean rollbackOnly;
            @Override public boolean isRollbackOnly() { return rollbackOnly; }
            @Override public void flush() { }
        }
    }
}
