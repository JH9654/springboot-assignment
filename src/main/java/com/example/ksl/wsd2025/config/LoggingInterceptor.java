package com.example.ksl.wsd2025.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        String query = request.getQueryString();
        System.out.printf("[%s] [REQUEST ] %s %s%s (client=%s)%n",
                LocalDateTime.now().format(FMT), request.getMethod(), request.getRequestURI(),
                query != null ? "?" + query : "", request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_TIME);
        long elapsed = start == null ? -1 : System.currentTimeMillis() - (long) start;
        System.out.printf("[%s] [RESPONSE] %s %s -> status=%d (%dms)%n",
                LocalDateTime.now().format(FMT), request.getMethod(), request.getRequestURI(),
                response.getStatus(), elapsed);
    }
}
