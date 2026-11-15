package com.codeoj.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeoj.common.entity.User;
import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.R;
import com.codeoj.common.result.ResultCode;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.security.LoginUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户管理接口（管理端补齐）：列表/搜索/提权降权/封禁解封/重置密码。
 * 封禁联动：登录拒绝（40101）+ 认证过滤器存在性/状态校验（既有 token 立即失效）。
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserMapper userMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 用户分页列表（keyword 模糊匹配用户名） */
    @GetMapping
    public R<Page<Map<String, Object>>> page(@RequestParam(name = "keyword", required = false) String keyword,
                                             @RequestParam(name = "page", defaultValue = "1") long page,
                                             @RequestParam(name = "size", defaultValue = "10") long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword);
        }
        wrapper.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        // 不回传密码哈希
        Page<Map<String, Object>> vo = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        vo.setRecords(result.getRecords().stream()
                .map(u -> Map.<String, Object>of(
                        "id", (Object) u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail() == null ? "" : u.getEmail(),
                        "role", u.getRole(),
                        "status", u.getStatus(),
                        "createdAt", String.valueOf(u.getCreatedAt())))
                .toList());
        return R.ok(vo);
    }

    /** 提权/降权（role: 0=学生 1=教师）；禁止对自身操作（防管理员锁死自己） */
    @PutMapping("/{id}/role")
    public R<Void> changeRole(@PathVariable(name = "id") Long id,
                              @RequestParam(name = "role") Integer role,
                              @AuthenticationPrincipal LoginUser operator) {
        requireNotSelf(id, operator);
        User u = requireUser(id);
        if (role == null || (role != 0 && role != 1)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "role 仅允许 0 或 1");
        }
        u.setRole(role);
        userMapper.updateById(u);
        return R.ok();
    }

    /** 封禁/解封（status: 0=正常 1=封禁）；禁止对自身操作（防管理员封禁自己） */
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable(name = "id") Long id,
                                @RequestParam(name = "status") Integer status,
                                @AuthenticationPrincipal LoginUser operator) {
        requireNotSelf(id, operator);
        User u = requireUser(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "status 仅允许 0 或 1");
        }
        u.setStatus(status);
        userMapper.updateById(u);
        return R.ok();
    }

    /** 管理员重置密码（设为临时密码，返回给教师转告学生） */
    @PutMapping("/{id}/password")
    public R<String> resetPassword(@PathVariable(name = "id") Long id) {
        User u = requireUser(id);
        String tempPassword = "oj" + String.valueOf(LocalDateTime.now().toLocalTime().toSecondOfDay());
        u.setPassword(passwordEncoder.encode(tempPassword));
        userMapper.updateById(u);
        return R.ok(tempPassword);
    }

    private void requireNotSelf(Long id, LoginUser operator) {
        if (operator != null && operator.userId().equals(id)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不能对自己执行此操作");
        }
    }

    private User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return u;
    }
}
