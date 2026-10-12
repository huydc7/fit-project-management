package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.DotDangKyForm;
import com.example.quanlydetai.dto.DotDangKyView;
import com.example.quanlydetai.security.AccountPrincipal;
import com.example.quanlydetai.service.DotDangKyService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registration-periods")
@PreAuthorize("hasRole('TRUONGKHOA')")
public class DotDangKyRestController {
    private final DotDangKyService service;

    public DotDangKyRestController(DotDangKyService service) {
        this.service = service;
    }

    @GetMapping
    public List<DotDangKyView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody DotDangKyForm body,
            @AuthenticationPrincipal AccountPrincipal user) {
        service.create(body, user.getMaGV());
    }
}
