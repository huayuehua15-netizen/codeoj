package com.codeoj.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.constant.RedisKeys;
import com.codeoj.common.entity.User;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.dto.LoginDTO;
import com.codeoj.server.dto.RegisterDTO;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.security.JwtUtil;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.security.TokenBlacklistService;
import com.codeoj.server.service.AuthService;
import com.codeoj.server.service.RateLimiter;
import com.codeoj.server.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现（论文 4.7/5.2）：
 * BCrypt 加盐存储；登录失败次数与锁定时长取自 {@code oj.rate-limit.*}（默认 5 次 / 15 分钟）；
 * 登出进入 Redis 黑名单直至令牌自然过期。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 原子计数：首次失败设置过期，返回当前计数（避免 INCR/EXPIRE 两步留下永不过期键） */
    private static final DefaultRedisScript<Long> COUNT_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
            + "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
            + "return c", Long.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimiter rateLimiter;
    private final StringRedisTemplate redis;
    private final TokenBlacklistService blacklistService;

    /** 登录失败多少次后锁定（API.md §1.2：连续失败 5 次） */
    @Value("${oj.rate-limit.login-max-attempts:5}")
    private long loginMaxAttempts;

    /** 锁定时长（分钟），传给 Lua 前需换算成秒 */
    @Value("${oj.rate-limit.login-lock-minutes:15}")
    private long loginLockMinutes;

    /** 登录按 IP 限流：次数/小时（API.md §1.2：30 次/小时） */
    @Value("${oj.rate-limit.login-per-hour:30}")
    private long loginPerHour;

    /** 注册按 IP 限流：次数/小时（API.md §1.1：10 次/小时） */
    @Value("${oj.rate-limit.register-per-hour:10}")
    private long registerPerHour;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RateLimiter rateLimiter,
                           StringRedisTemplate redis,
                           TokenBlacklistService blacklistService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.redis = redis;
        this.blacklistService = blacklistService;
    }

    @Override
    public void register(RegisterDTO dto, String clientIp) {
        // 注册按 IP 限流：每小时最多 N 次，防脚本批量刷号（正常人工注册远用不完；论文 4.7 限流机制）
        rateLimiter.check("register", clientIp, (int) registerPerHour, 3600);
        // 先查一次只为给出友好提示（覆盖绝大多数串行场景），真正的唯一性由下面的
        // DuplicateKeyException 兜底——selectCount 与 insert 之间没有事务隔离可依赖，
        // 两个并发注册同名用户可以同时通过这一步检查（review P2-38 的 TOCTOU）。
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户名已被占用");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_user_username 是最终裁判：并发下输的那一方到这里，转成与先查
            // 分支一致的业务码，而不是漏成 50000 让用户以为注册接口挂了。
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户名已被占用");
        }
        // 不签发 JWT：API.md §1.1 明确注册不自动登录、响应 data:null。
        // 这里曾经签发后又被 Controller 丢弃，等于白算一次 HS256 签名。
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 登录按 IP 限流：每小时最多 N 次，防密码喷洒（论文 4.7 滑动窗口限流机制）
        rateLimiter.check("login", clientIp, (int) loginPerHour, 3600);
        // 登录失败锁定：失败 N 次锁 M 分钟（论文 4.7 / 测试 F-02）。
        // 键同时包含 IP：只用 username 做键时，任何人知道教师账号就能连发 N 次
        // 错误密码把该账号锁死 M 分钟（每次失败还续期），构成未授权 DoS。
        // 键名走 RedisKeys（CONTRACT §5 禁止硬编码键名），格式与原先手写值完全一致，
        // 已有键不会因为这次改动而失效。
        String lockKey = RedisKeys.rate("login:fail", clientIp + ":" + dto.getUsername());
        String failsRaw = redis.opsForValue().get(lockKey);
        long fails = failsRaw != null ? Long.parseLong(failsRaw) : 0;
        if (fails >= loginMaxAttempts) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // 原子计数：INCR 与 EXPIRE 分两步时，进程中断会留下永不过期的计数键。
            // yml 里的锁定时长单位是分钟，Lua 的 EXPIRE 要秒，故这里 *60。
            Long now = redis.execute(COUNT_WITH_TTL, List.of(lockKey),
                    String.valueOf(loginLockMinutes * 60));
            log.debug("login failed, count={}", now);
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        // 封禁账号拒绝登录（论文 4.7 安全设计延伸，管理员在用户管理中操作）
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "账号已被封禁，请联系教师");
        }
        redis.delete(lockKey);
        String token = jwtUtil.issue(user.getId(), user.getRole());
        return LoginVO.of(token, user);
    }

    @Override
    public void logout(LoginUser user, String token) {
        // 拉黑放在 service 而不是 controller：黑名单是"登出"这件事的组成部分，
        // 任何调用登出的入口都不该漏掉它；controller 只负责从请求里取出裸 token。
        log.info("logout, userId={}", user.userId());
        revokeToken(token);
    }

    @Override
    public LoginVO getUserInfo(LoginUser user) {
        User u = userMapper.selectById(user.userId());
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return LoginVO.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .build();
    }

    @Override
    public void updateEmail(LoginUser user, String email) {
        // 个人信息维护（论文表 3-1）：仅邮箱可改，用户名与角色不可变更
        User u = userMapper.selectById(user.userId());
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        u.setEmail(email);
        u.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(u);
        log.info("email updated, userId={}", user.userId());
    }

    @Override
    public void changePassword(LoginUser user, String oldPassword, String newPassword, String token) {
        User u = userMapper.selectById(user.userId());
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, u.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "原密码错误");
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(u);
        log.info("password changed, userId={}", user.userId());
        // 改密成功后才拉黑：失败分支（旧密码不对）必须让当前会话继续可用，
        // 否则用户输错一次旧密码就把自己踢下线了。
        revokeToken(token);
    }

    /**
     * 把令牌的 jti 加入黑名单直到它自然过期（论文 4.7/5.2）。
     *
     * <p>token 允许为 null：未携带 Authorization 头时没有可拉黑的令牌，
     * 此时登出依然算成功（客户端丢弃令牌即可）。解析失败同理——
     * 一个已经无效的令牌本来就不需要再拉黑一次。
     */
    private void revokeToken(String token) {
        if (token == null) {
            return;
        }
        jwtUtil.tryParse(token).ifPresent(claims ->
                blacklistService.add(claims.getId(), jwtUtil.remainingMs(token)));
    }
}
