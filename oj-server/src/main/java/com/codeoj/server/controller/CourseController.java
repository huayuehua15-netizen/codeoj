package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import jakarta.validation.Valid;
import com.codeoj.server.dto.CourseFormDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程实验接口（论文 3.3/表 3-3：课程创建、题目分组、完成情况统计，管理员）。
 */
@RestController
@RequestMapping("/api/admin/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> create(@Valid @RequestBody CourseFormDTO dto,
                          @AuthenticationPrincipal LoginUser user) {
        return R.ok(courseService.create(user.userId(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable("id") Long id, @Valid @RequestBody CourseFormDTO dto) {
        courseService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable("id") Long id) {
        courseService.delete(id);
        return R.ok();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> list() {
        return R.ok(courseService.listAll());
    }

    /** 课程内题目分组 */
    @GetMapping("/{id}/problems")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> problems(@PathVariable("id") Long id) {
        return R.ok(courseService.problems(id));
    }

    /** 班级完成情况统计 */
    @GetMapping("/{id}/completion")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> completion(@PathVariable("id") Long id) {
        return R.ok(courseService.completion(id));
    }
}
