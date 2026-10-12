package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.BoMonRequest;
import com.example.quanlydetai.dto.BoNhiemRequest;
import com.example.quanlydetai.service.TaiKhoanService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class QuanTriDanhMucRestController {
    private final TaiKhoanService service;

    public QuanTriDanhMucRestController(TaiKhoanService service) {
        this.service = service;
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public void department(@Valid @RequestBody BoMonRequest body) {
        service.addDepartment(body.id(), body.name());
    }

    @PutMapping("/dean")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dean(@Valid @RequestBody BoNhiemRequest body) {
        service.dean(body.teacher(), body.appointed());
    }
}
