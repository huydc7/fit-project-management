package com.example.quanlydetai.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Fixed messages at the filter boundary, before MVC exception handling is available. */
public final class ApiSecurityResponse {
    private ApiSecurityResponse() {}

    public static void write(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        String title = status == 401 ? "Unauthorized" : "Forbidden";
        String detail =
                status == 401
                        ? "Vui lòng đăng nhập lại."
                        : "Không có quyền truy cập hoặc CSRF token không hợp lệ.";
        response.getWriter()
                .write(
                        "{\"type\":\"about:blank\",\"title\":\""
                                + title
                                + "\",\"status\":"
                                + status
                                + ",\"detail\":\""
                                + detail
                                + "\"}");
    }
}
