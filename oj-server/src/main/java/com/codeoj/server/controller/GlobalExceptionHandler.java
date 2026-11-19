package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;

/**
 * 全局异常处理：转换为统一返回结构 {code, message, data}（论文 4.5）。
 *
 * <p><b>为什么要把参数类异常逐个列出来（review P2-29）</b>：契约 docs/API.md §0.3 规定
 * 参数类错误统一 40000。原先只兜 {@code MethodArgumentNotValidException}，
 * 其余（JSON 语法错、{@code ?status=abc} 类型不匹配、缺必填参数、上传超限）全部落到
 * {@code Exception} 兜底变成 50000，前端按"系统繁忙"提示，用户看到的是服务故障而不是
 * 自己填错了——既误导用户，也让真正需要关注的 5xx 告警被这类噪音淹没。
 *
 * <p>HTTP 状态码在这里统一由返回体承载（{@code Result.code}），HTTP 层仍为 200，
 * 与 API.md §0.2「HTTP 状态码恒为 200，业务语义全部由 code 表达」一致。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.codeoj.common.exception.BizException.class)
    public R<Void> handleBiz(com.codeoj.common.exception.BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("请求参数错误");
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 请求体无法解析：JSON 语法错误、字段类型不匹配（如 difficulty 传了字符串）。
     * 归属客户端错误，绝不能记成系统错误——这里刻意不打堆栈，避免正常的脏请求刷爆日志。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.debug("unreadable request body: {}", e.getMessage());
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /**
     * 查询参数类型不匹配：{@code ?status=abc} 传给 Integer/Long。
     * 报出参数名便于前端定位，而不是笼统的"参数错误"。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "参数 " + e.getName() + " 格式错误");
    }

    /** 缺少必填查询参数（@RequestParam(required=true) 未传） */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数 " + e.getParameterName());
    }

    /** 上传体积超限：Spring Boot 在 multipart 解析阶段就抛出，走不到方法体 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "上传文件过大");
    }

    /**
     * 表单/查询参数绑定失败（非 @RequestBody 路径）。
     * BindException 是 MethodArgumentNotValidException 的父类，单独声明以覆盖
     * 旧式 ModelAttribute 绑定场景；Spring 会优先匹配更具体的子类处理器。
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("请求参数错误");
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * Spring 6.1 起 @RequestParam/@PathVariable 上的约束校验抛此异常
     * （与 @Valid @RequestBody 的 MethodArgumentNotValidException 分开）。
     * 当前 oj-server 的 controller 尚未使用参数级约束，但一旦加上就会命中这里，
     * 提前接住可避免它退化回 50000。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("请求参数错误");
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 方法级约束校验失败（@Validated 类上的 @NotBlank 等），同样是客户端错误 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("请求参数错误");
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    /**
     * 兜底：走到这里的才是真正需要排查的服务端异常（review P2-30）。
     * 原实现只打堆栈不打请求信息，线上只知道"发生过一次 NPE"，不知道是哪个接口、
     * 什么方法——因此在日志里补上 method + URI（不含 header/body，避免泄露令牌与密码）。
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e, HttpServletRequest request) {
        log.error("unexpected error, {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
