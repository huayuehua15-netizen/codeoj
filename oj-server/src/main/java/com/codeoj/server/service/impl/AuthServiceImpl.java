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
import com.codeoj.server.service.AuthService;
import com.codeoj.server.service.RateLimiter;
import com.codeoj.server.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 认证服务实现（论文 4.7/5.2）：
 * BCrypt 加盐存储；登录失败 5 次锁定 15 分钟；登出进入 Redis 黑名单直至令牌自然过期。
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

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RateLimiter rateLimiter,
                           StringRedisTemplate redis) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.redis = redis;
    }

    @Override
    public LoginVO register(RegisterDTO dto, String clientIp) {
        // 注册按 IP 限流：每小时最多 10 次，防脚本批量刷号（正常人工注册远用不完；论文 4.7 限流机制）
        rateLimiter.check("register", clientIp, 10, 3600);
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
        userMapper.insert(user);
        String token = jwtUtil.issue(user.getId(), user.getRole());
        return LoginVO.of(token, user);
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 登录按 IP 限流：每小时最多 30 次，防密码喷洒（论文 4.7 滑动窗口限流机制）
        rateLimiter.check("login", clientIp, 30, 3600);
        // 登录失败锁定：失败 5 次锁 15 分钟（论文 4.7 / 测试 F-02）。
        // 键同时包含 IP：只用 username 做键时，任何人知道教师账号就能连发 5 次
        // 错误密码把该账号锁死 15 分钟（每次失败还续期），构成未授权 DoS。
        String lockKey = "oj:rate:login:fail:" + clientIp + ":" + dto.getUsername();
        String failsRaw = redis.opsForValue().get(lockKey);
        long fails = failsRaw != null ? Long.parseLong(failsRaw) : 0;
        if (fails >= 5) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // 原子计数：INCR 与 EXPIRE 分两步时，进程中断会留下永不过期的计数键
            Long now = redis.execute(COUNT_WITH_TTL, List.of(lockKey), "900");
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
    public void logout(LoginUser user) {
        // TODO 第二阶段：logout 接口需拿到原始 token 才能计算剩余 TTL；
        //  由 AuthController 解析 Authorization 头后调用本方法 + TokenBlacklistService.add(jti, remainingMs)
        log.info("logout, userId={}", user.userId());
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
    public void changePassword(LoginUser user, String oldPassword, String newPassword) {
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
    }
}
