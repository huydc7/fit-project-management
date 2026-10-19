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
    public List<DotDangKyView> list(@RequestParam(defaultValue = "") String q) {
        return service.search(q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody DotDangKyForm body,
            @AuthenticationPrincipal AccountPrincipal user) {
        service.create(body, user.getMaGV());
    }

    @GetMapping("/{id}")
    public DotDangKyForm detail(@PathVariable String id) {
        return service.editForm(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable String id,
            @Valid @RequestBody DotDangKyForm body,
            @AuthenticationPrincipal AccountPrincipal user) {
        service.update(id, body, user.getMaGV());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, @AuthenticationPrincipal AccountPrincipal user) {
        service.delete(id, user.getMaGV());
    }
}
