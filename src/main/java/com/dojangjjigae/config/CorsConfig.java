package com.dojangjjigae.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * React 프론트(로컬 5173, 배포된 Vercel 도메인)에서의 API 호출을 허용한다.
 * 허용 도메인은 application.yml 의 app.cors.allowed-origins 에서 관리하며,
 * 배포 후에는 CORS_ALLOWED_ORIGINS 환경변수로 실제 Vercel 주소를 넣어준다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
