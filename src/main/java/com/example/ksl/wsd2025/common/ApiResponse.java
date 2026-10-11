package com.example.ksl.wsd2025.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 표준 응답 포맷 (과제 예시 + 03주차 강의 오류 응답 형식 결합)
 * 성공: { "status": "success", "data": {...} }
 * 실패: { "status": "error", "statusCode": 404, "message": "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String status;
    private final Integer statusCode;
    private final T data;
    private final String message;

    private ApiResponse(String status, Integer statusCode, T data, String message) {
        this.status = status;
        this.statusCode = statusCode;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", null, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", null, data, message);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return new ApiResponse<>("error", statusCode, null, message);
    }

    public String getStatus() { return status; }
    public Integer getStatusCode() { return statusCode; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
