package com.codeoj.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（论文 2.1）：通用 Mapper 与分页插件。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件 + 单页上限。
     *
     * <p><b>为什么这里没有 MetaObjectHandler</b>：时间字段（created_at/updated_at）
     * 由各业务层显式赋值（{@code setCreatedAt(LocalDateTime.now())}，
     * updated_at 同理），不启用 MP 自动填充。实体上写的是
     * {@code @TableField("created_at")} 这类显式列名，并没有任何字段声明
     * {@code @TableField(fill = FieldFill.INSERT)}；而 MP 只在字段带 fill 策略时
     * 才回调 handler，因此注册一个 handler 不会生效——它只会让读者误以为
     * "时间字段会自动填"，掩盖了业务层真实的手工赋值。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页上限防护：恶意 size=99999 的一次请求会拖垮列表查询
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
