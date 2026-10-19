package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.dto.HoiDongResponse;
import com.example.quanlydetai.dto.PhanCongHoiDongRequest;
import com.example.quanlydetai.dto.PhanCongPhanBienRequest;
import com.example.quanlydetai.service.HoiDongService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/councils")
@PreAuthorize("hasRole('TRUONGKHOA')")
public class HoiDongRestController {
    private final HoiDongService service;

    public HoiDongRestController(HoiDongService service) {
        this.service = service;
    }

    @GetMapping
    public List<HoiDongResponse> all() {
        return service.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody HoiDongRequest body) {
        service.create(body);
    }

    @PatchMapping("/{id}/finalize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizeCouncil(@PathVariable String id) {
        service.finalizeCouncil(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody HoiDongRequest body) {
        service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deleteDraft(id);
    }

    @PutMapping("/topics/{id}/reviewer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignReviewer(
            @PathVariable String id, @Valid @RequestBody PhanCongPhanBienRequest body) {
        service.assignReviewer(id, body.teacher());
    }

    @PutMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignCouncil(
            @PathVariable String id, @Valid @RequestBody PhanCongHoiDongRequest body) {
        service.assignCouncil(id, body.council());
    }
}
