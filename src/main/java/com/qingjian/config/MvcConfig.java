package com.qingjian.config;

import com.qingjian.utils.LoginInterceptor;
import com.qingjian.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器：精确放行只读接口，写接口必须登录
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        // 商户相关 - 只放行查询类接口
                        "/shop/{id}",
                        "/shop/list",
                        "/shop/of/name",
                        "/shop/of/type",
                        "/shop/nearby",
                        "/shop/recommend",
                        "/shop/rank",
                        // 优惠券相关 - 只放行查询类接口
                        "/voucher/list/{shopId}",
                        // 商户类型
                        "/shop-type/**",
                        // 图片上传
                        "/upload/**",
                        // 博客热门
                        "/blog/hot",
                        "/blog/{id}",
                        "/blog/comments/{id}",
                        // 用户登录相关
                        "/user/code",
                        "/user/login",
                        "/user/shop/{id}",
                        // 数据初始化
                        "/init/**",
                        // 评价相关 - 只放行查询
                        "/shop-comment/list/**",
                        "/shop-comment/tags/**"
                ).order(1);
        // token刷新的拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
