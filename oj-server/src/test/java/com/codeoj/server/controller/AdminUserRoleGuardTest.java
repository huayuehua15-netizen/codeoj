package com.codeoj.server.controller;

import com.codeoj.common.entity.User;
import com.codeoj.common.exception.BizException;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 最后一个管理员不能被降权（review P2-39）。
 *
 * <p>这是"改数据就能救回来"之外的唯一软防线：一旦 role=1 的账号归零，
 * 管理后台再没有任何人能进入，只能直接改库。
 */
class AdminUserRoleGuardTest {

    UserMapper users = mock(UserMapper.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AdminUserController controller = new AdminUserController(users, encoder);

    /** 操作者与被操作者不同（否则会先被 requireNotSelf 拦下，测不到本规则） */
    LoginUser operator = new LoginUser(1L, 1, "jti");

    private static User user(Long id, Integer role) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        u.setRole(role);
        return u;
    }

    @Test void demotingTheOnlyAdminIsRejected() {
        when(users.selectById(2L)).thenReturn(user(2L, 1));
        when(users.selectCount(any())).thenReturn(1L);

        BizException e = assertThrows(BizException.class,
                () -> controller.changeRole(2L, 0, operator));
        assertEquals(40300, e.getCode());
        assertTrue(e.getMessage().contains("最后一个管理员"));
        // 关键断言：被拒绝时绝不能落库
        verify(users, never()).updateById(any(User.class));
    }

    @Test void demotingOneOfSeveralAdminsIsAllowed() {
        when(users.selectById(2L)).thenReturn(user(2L, 1));
        when(users.selectCount(any())).thenReturn(2L);

        assertDoesNotThrow(() -> controller.changeRole(2L, 0, operator));
        verify(users).updateById(any(User.class));
    }

    @Test void promotingAStudentNeverCountsAdmins() {
        when(users.selectById(3L)).thenReturn(user(3L, 0));

        assertDoesNotThrow(() -> controller.changeRole(3L, 1, operator));
        // 提权不涉及"管理员归零"，不该产生额外查询
        verify(users, never()).selectCount(any());
        verify(users).updateById(any(User.class));
    }

    @Test void promotingAnExistingAdminOutOfTheRuleIsAlsoSkipped() {
        when(users.selectById(4L)).thenReturn(user(4L, 1));

        assertDoesNotThrow(() -> controller.changeRole(4L, 1, operator));
        verify(users, never()).selectCount(any());
    }
}
