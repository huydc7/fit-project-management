package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.dto.HoiDongResponse;
import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.dto.PhanCongHoiDongRequest;
import com.example.quanlydetai.dto.PhanCongPhanBienRequest;
import com.example.quanlydetai.service.HoiDongService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<HoiDongResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.all(Paging.request(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody HoiDongRequest body) {
        service.create(body);
    }

    @GetMapping("/topics/{id}/candidates")
    public List<LuaChonResponse> candidates(@PathVariable String id) {
        return service.candidates(id);
    }

    @PostMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public void createForTopic(@PathVariable String id, @Valid @RequestBody HoiDongRequest body) {
        service.createForTopic(id, body);
    }

    @PatchMapping("/{id}/finalize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizeCouncil(@PathVariable String id, @RequestParam(required = false) String topic) {
        if (topic == null) service.finalizeCouncil(id);
        else service.finalizeForTopic(id, topic);
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
