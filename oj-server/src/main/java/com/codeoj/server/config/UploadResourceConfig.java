package com.codeoj.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 上传文件的静态映射（API.md v1.8）：/uploads/** → 磁盘目录，公开可读。
 *
 * <p>头像这类静态资源由服务端直接吐文件而不是走业务接口，是为了：
 * ① 能被浏览器与 Nginx 缓存；② 列表接口只存 URL 字符串，响应体保持轻量。
 * 文件名由服务端生成（见 AuthServiceImpl.updateAvatar），不存在路径穿越面。
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    /** 与 oj.upload.avatar-dir 的父目录保持一致（avatar-dir 默认在其下） */
    @Value("${oj.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(uploadRootDir).toAbsolutePath().normalize();
        // URL 末尾必须有 /，ResourceHandler 按"目录前缀"匹配
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root + "/");
    }
}
