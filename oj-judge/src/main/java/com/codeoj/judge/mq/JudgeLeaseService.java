package com.codeoj.judge.mq;
import com.codeoj.judge.mapper.SubmissionMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;

/** Renewal has its own executor; slow recovery/MQ calls cannot expire live workers. */
@Component
public class JudgeLeaseService {
    private static final Logger log = LoggerFactory.getLogger(JudgeLeaseService.class);
    private final SubmissionMapper mapper;
    private final int leaseSeconds;
    private final ScheduledExecutorService renewer = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "judge-lease-renewer");
        thread.setDaemon(true);
        return thread;
    });
    public JudgeLeaseService(SubmissionMapper mapper, @Value("${oj.judge.lease-seconds:120}") int leaseSeconds) {
        if (leaseSeconds < 10) throw new IllegalArgumentException("judge lease must be at least 10 seconds");
        this.mapper = mapper;
        this.leaseSeconds = leaseSeconds;
    }
    public int leaseSeconds() { return leaseSeconds; }
    public AutoCloseable keepAlive(long sid, String token) {
        var future = renewer.scheduleWithFixedDelay(() -> {
            try {
                if (mapper.renewLease(sid, token, leaseSeconds) == 0)
                    log.warn("judge lease lost; result will be fenced, submission={}", sid);
            } catch (Exception e) { log.warn("judge lease renewal failed, submission={}", sid, e); }
        }, leaseSeconds / 3, leaseSeconds / 3, TimeUnit.SECONDS);
        return () -> future.cancel(false);
    }
    @PreDestroy
    public void close() { renewer.shutdownNow(); }
}
