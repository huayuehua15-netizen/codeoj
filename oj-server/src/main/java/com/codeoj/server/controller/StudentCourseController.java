package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.dto.JoinCourseDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.CourseService;
import com.codeoj.server.service.RateLimiter;
import com.codeoj.server.vo.CourseDetailVO;
import com.codeoj.server.vo.StudentCourseVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生课程班级接口（论文 3.3 的班级侧）：
 * 我的课程、邀请码加入、课程详情、退出。
 *
 * <p>所有路径都需要登录（SecurityConfig 默认 anyRequest().authenticated()），
 * 课程详情额外要求"班级成员"或管理员；未加入的学生拿不到课程内容。
 */
@RestController
@RequestMapping("/api/courses")
public class StudentCourseController {

    private final CourseService courseService;
    private final RateLimiter rateLimiter;

    public StudentCourseController(CourseService courseService, RateLimiter rateLimiter) {
        this.courseService = courseService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public R<List<StudentCourseVO>> myCourses(@AuthenticationPrincipal LoginUser user) {
        return R.ok(courseService.myCourses(user));
    }

    @PostMapping("/join")
    public R<StudentCourseVO> join(@Valid @RequestBody JoinCourseDTO dto,
                                   @AuthenticationPrincipal LoginUser user) {
        // 邀请码是可枚举的短串，按登录用户限流，避免脚本暴力尝试班级码
        rateLimiter.check("course:join", String.valueOf(user.userId()), 10, 60);
        return R.ok(courseService.join(user, dto.getCode()));
    }

    @GetMapping("/{id}")
    public R<CourseDetailVO> detail(@PathVariable("id") Long id,
                                    @AuthenticationPrincipal LoginUser user) {
        return R.ok(courseService.detail(id, user));
    }

    @PostMapping("/{id}/leave")
    public R<Void> leave(@PathVariable("id") Long id,
                         @AuthenticationPrincipal LoginUser user) {
        courseService.leave(id, user);
        return R.ok();
    }
}
