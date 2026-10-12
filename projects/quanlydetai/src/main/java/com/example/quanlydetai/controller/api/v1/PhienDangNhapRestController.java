package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.DoiMatKhauRequest;
import com.example.quanlydetai.service.TaiKhoanService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
public class PhienDangNhapRestController {
    private final TaiKhoanService accounts;

    public PhienDangNhapRestController(TaiKhoanService accounts) {
        this.accounts = accounts;
    }

    public record CsrfResponse(String headerName, String parameterName, String token) {}

    @GetMapping("/csrf")
    public org.springframework.http.ResponseEntity<CsrfResponse> csrf(
            org.springframework.security.web.csrf.CsrfToken token) {
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(
                        new CsrfResponse(
                                token.getHeaderName(), token.getParameterName(), token.getToken()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(
            @Valid @RequestBody DoiMatKhauRequest body,
            jakarta.servlet.http.HttpServletRequest request) {
        accounts.changePassword(body);
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
