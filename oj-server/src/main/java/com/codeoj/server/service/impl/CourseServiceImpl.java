package com.codeoj.server.service.impl;

import com.codeoj.common.entity.Course;
import com.codeoj.common.entity.CourseProblem;
import com.codeoj.server.dto.CourseFormDTO;
import com.codeoj.server.mapper.CourseMapper;
import com.codeoj.server.mapper.CourseProblemMapper;
import com.codeoj.server.service.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程实验服务实现（骨架）。
 */
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseProblemMapper courseProblemMapper;

    public CourseServiceImpl(CourseMapper courseMapper, CourseProblemMapper courseProblemMapper) {
        this.courseMapper = courseMapper;
        this.courseProblemMapper = courseProblemMapper;
    }

    @Override
    @Transactional
    public Long create(Long teacherId, CourseFormDTO dto) {
        Course course = new Course();
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setTeacherId(teacherId);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);
        bindProblems(course.getId(), dto.getProblemIds());
        return course.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CourseFormDTO dto) {
        Course course = getById(id);
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
        courseProblemMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, id));
        bindProblems(id, dto.getProblemIds());
    }

    private void bindProblems(Long courseId, List<Long> problemIds) {
        // TODO 第二阶段：校验题目存在性
        if (problemIds == null) {
            return;
        }
        for (int i = 0; i < problemIds.size(); i++) {
            CourseProblem cp = new CourseProblem();
            cp.setCourseId(courseId);
            cp.setProblemId(problemIds.get(i));
            cp.setSortOrder(i + 1);
            courseProblemMapper.insert(cp);
        }
    }

    @Override
    public void delete(Long id) {
        getById(id);
        courseMapper.deleteById(id);
        courseProblemMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, id));
    }

    @Override
    public List<Course> listAll() {
        return courseMapper.selectList(null);
    }

    @Override
    public Course getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new com.codeoj.common.exception.BizException(
                    com.codeoj.common.result.ResultCode.NOT_FOUND);
        }
        return course;
    }

    @Override
    public List<CourseProblem> problems(Long courseId) {
        return courseProblemMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseProblem>()
                .eq(CourseProblem::getCourseId, courseId)
                .orderByAsc(CourseProblem::getSortOrder));
    }

    @Override
    public List<CompletionItem> completion(Long courseId) {
        // TODO 第二阶段：按课程题目聚合提交/通过人数（论文 3.3 班级完成情况）
        return List.of();
    }
}
