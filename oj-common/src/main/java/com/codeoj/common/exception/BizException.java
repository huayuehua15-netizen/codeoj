package com.codeoj.common.exception;

import com.codeoj.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务状态码，由全局异常处理器转换为统一返回结构。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
