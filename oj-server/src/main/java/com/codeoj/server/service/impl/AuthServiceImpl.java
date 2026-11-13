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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证服务实现（论文 4.7/5.2）：
 * BCrypt 加盐存储；登录失败 5 次锁定 15 分钟；登出进入 Redis 黑名单直至令牌自然过期。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

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
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户名已被占用");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.insert(user);
        String token = jwtUtil.issue(user.getId(), user.getRole());
        return LoginVO.of(token, user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 登录限流：失败 5 次锁定 15 分钟（论文 4.7 / 测试 F-02）
        String lockKey = "oj:rate:login:fail:" + dto.getUsername();
        String failsRaw = redis.opsForValue().get(lockKey);
        long fails = failsRaw != null ? Long.parseLong(failsRaw) : 0;
        if (fails >= 5) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            redis.opsForValue().increment(lockKey);
            redis.expire(lockKey, Duration.ofMinutes(15));
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
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
                .nickname(u.getNickname())
                .role(u.getRole())
                .build();
    }
}
