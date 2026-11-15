package com.codeoj.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeoj.common.entity.Announcement;
import com.codeoj.common.result.R;
import com.codeoj.server.mapper.AnnouncementMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公告接口（教学场景：实验截止提醒、平台通知）——论文 3.3 教学场景配套。
 * 列表公开（全站展示）；发布/删除需教师。
 */
@RestController
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementController(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /** 公告列表（公开，倒序取最新） */
    @GetMapping("/api/announcements")
    public R<List<Map<String, Object>>> list() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId));
        return R.ok(list.stream()
                .map(a -> Map.<String, Object>of(
                        "id", (Object) a.getId(),
                        "title", a.getTitle(),
                        "content", a.getContent(),
                        "createdAt", String.valueOf(a.getCreatedAt())))
                .toList());
    }

    @PostMapping("/api/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Long> publish(@RequestBody Map<String, String> body,
                           @AuthenticationPrincipal com.codeoj.server.security.LoginUser user) {
        String title = body.getOrDefault("title", "").trim();
        String content = body.getOrDefault("content", "").trim();
        if (title.isEmpty() || content.isEmpty()) {
            return R.fail(40000, "标题与内容不能为空");
        }
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setCreatedBy(user.userId());
        a.setCreatedAt(LocalDateTime.now());
        announcementMapper.insert(a);
        return R.ok(a.getId());
    }

    @DeleteMapping("/api/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable(name = "id") Long id) {
        announcementMapper.deleteById(id);
        return R.ok();
    }
}
