package com.codeoj.judge.core.sandbox;

/** Docker transport or lifecycle failure, separate from a terminal program verdict. */
public final class RetryableSandboxException extends RuntimeException {
    public RetryableSandboxException(Throwable cause) {
        super("Temporary sandbox infrastructure failure", cause);
    }
}
