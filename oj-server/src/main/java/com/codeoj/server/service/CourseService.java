package com.codeoj.server.service;

import com.codeoj.common.entity.Course;
import com.codeoj.common.entity.CourseProblem;
import com.codeoj.server.dto.CourseFormDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.vo.CourseDetailVO;
import com.codeoj.server.vo.CourseRosterVO;
import com.codeoj.server.vo.CourseVO;
import com.codeoj.server.vo.StudentCourseVO;

import java.util.List;

/**
 * 课程班级服务（论文 3.3：课程创建、题目分组、完成情况统计）。
 *
 * <p>课程已从"管理员题组"升级为"教师班级"：邀请码加入、截止时间、学生进度。
 */
public interface CourseService {

    Long create(Long teacherId, CourseFormDTO dto);

    void update(Long id, CourseFormDTO dto);

    void delete(Long id);

    List<CourseVO> listAll();

    Course getById(Long id);

    /** 课程内题目列表（按 sortOrder 排序） */
    List<CourseProblem> problems(Long courseId);

    /** 班级完成情况：每题提交/通过/按时通过人数（仅统计班级成员） */
    List<CompletionItem> completion(Long courseId);

    /** 学生已加入的课程 + 我的完成度 */
    List<StudentCourseVO> myCourses(LoginUser user);

    /** 邀请码加入；重复加入幂等 */
    StudentCourseVO join(LoginUser user, String code);

    /** 学生查看课程详情；管理员可预览，普通用户必须是班级成员 */
    CourseDetailVO detail(Long courseId, LoginUser user);

    /** 学生退出课程 */
    void leave(Long courseId, LoginUser user);

    /** 教师班级学情（成员 × 题目矩阵） */
    CourseRosterVO roster(Long courseId);

    /** 教师移除成员 */
    void removeMember(Long courseId, Long userId);

    /** 轮换邀请码并返回新码 */
    String rotateJoinCode(Long courseId);

    record CompletionItem(Long problemId, String title, long memberCount,
                          long submittedUsers, long acceptedUsers, long onTimeAcceptedUsers) {
    }
}
