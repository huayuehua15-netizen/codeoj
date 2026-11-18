package com.codeoj.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** 按提交时的顺序冻结判题参数。此类型只在服务内部使用。 */
public record JudgeSnapshot(Integer timeLimitMs, Integer memoryLimitMb, List<OneCase> cases) {
    public static final int MAX_CASES = 1000;
    public static final int MAX_PAYLOAD_BYTES = 32 * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    public record OneCase(String input, String expectedOutput) { }

    public byte[] encode() {
        try {
            return JSON.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize judge snapshot", e);
        }
    }

    public static JudgeSnapshot decode(String payload) {
        try {
            return JSON.readValue(payload, JudgeSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid judge snapshot", e);
        }
    }
}
