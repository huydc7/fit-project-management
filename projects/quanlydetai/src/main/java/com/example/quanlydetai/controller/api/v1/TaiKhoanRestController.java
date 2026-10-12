package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.DatLaiMatKhauRequest;
import com.example.quanlydetai.dto.TaiKhoanRequest;
import com.example.quanlydetai.dto.TaiKhoanResponse;
import com.example.quanlydetai.dto.TrangThaiTaiKhoanRequest;
import com.example.quanlydetai.service.TaiKhoanService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class TaiKhoanRestController {
    private final TaiKhoanService service;

    public TaiKhoanRestController(TaiKhoanService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaiKhoanResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody TaiKhoanRequest body) {
        service.create(body);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void active(@PathVariable String id, @Valid @RequestBody TrangThaiTaiKhoanRequest body) {
        service.setActive(id, body.active());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable String id, @Valid @RequestBody DatLaiMatKhauRequest body) {
        service.resetPassword(id, body.password());
    }
}
