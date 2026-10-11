package com.example.ksl.wsd2025.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final MaintenanceInterceptor maintenanceInterceptor;

    public WebConfig(LoggingInterceptor loggingInterceptor, MaintenanceInterceptor maintenanceInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.maintenanceInterceptor = maintenanceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 순서: 로그 → 점검모드 (503 응답도 로그에 남도록)
        registry.addInterceptor(loggingInterceptor).order(1).addPathPatterns("/api/**");
        registry.addInterceptor(maintenanceInterceptor).order(2).addPathPatterns("/api/**");
    }
}
