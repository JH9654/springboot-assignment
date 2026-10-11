package com.example.ksl.wsd2025.config;

import com.example.ksl.wsd2025.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/** 점검 모드: app.maintenance=true 또는 헤더 X-Maintenance: true → 503 */
@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final boolean maintenance;

    public MaintenanceInterceptor(ObjectMapper objectMapper,
                                  @Value("${app.maintenance:false}") boolean maintenance) {
        this.objectMapper = objectMapper;
        this.maintenance = maintenance;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        boolean headerFlag = "true".equalsIgnoreCase(request.getHeader("X-Maintenance"));
        if (!maintenance && !headerFlag) return true;

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", "60");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(503, "서버 점검 중입니다. 잠시 후 다시 시도해주세요.")));
        return false; // 컨트롤러로 진행하지 않음
    }
}
