package com.codeoj.server.service;

import com.codeoj.common.entity.Course;
import com.codeoj.common.entity.CourseProblem;
import com.codeoj.server.dto.CourseFormDTO;

import java.util.List;

/**
 * 课程实验服务（论文表 3-3：课程创建、题目分组、完成情况统计）。
 */
public interface CourseService {

    Long create(Long teacherId, CourseFormDTO dto);

    void update(Long id, CourseFormDTO dto);

    void delete(Long id);

    List<Course> listAll();

    Course getById(Long id);

    /** 课程内题目列表（按 sortOrder 排序） */
    List<CourseProblem> problems(Long courseId);

    /** 班级完成情况：每题的提交人数与通过人数 */
    List<CompletionItem> completion(Long courseId);

    record CompletionItem(Long problemId, String title, long submittedUsers, long acceptedUsers) {
    }
}
