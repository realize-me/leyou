package com.leyou.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter(){
        // 1. 添加CORS配置信息
        CorsConfiguration corsConfig = new CorsConfiguration();
        // 1) 允许的域，不要写*，否则Cookie就不能用了
        corsConfig.addAllowedOrigin("http://manage.leyou.com");
        corsConfig.addAllowedOrigin("http://www.leyou.com");
        // 2) 是否发送Cookie信息
        corsConfig.setAllowCredentials(true);
        // 3) 允许的请求方式
        corsConfig.addAllowedMethod("OPTIONS");
        corsConfig.addAllowedMethod("HEAD");
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("PUT");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedMethod("DELETE");
        corsConfig.addAllowedMethod("PATCH");
        // 4) 允许的头信息
        corsConfig.addAllowedHeader("*");
        // 5) 设置有效时长
        corsConfig.setMaxAge(3600L);
        // 2. 添加映射路径，拦截所有请求
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", corsConfig);

        // 3. 返回新的CorsFilter
        return new CorsFilter(configSource);
    }


}
