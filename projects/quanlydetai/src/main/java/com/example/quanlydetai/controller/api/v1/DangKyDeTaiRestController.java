package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.DangKyDeTaiRequest;
import com.example.quanlydetai.dto.DangKyDeTaiResponse;
import com.example.quanlydetai.dto.DuyetRequest;
import com.example.quanlydetai.service.NhomService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
public class DangKyDeTaiRestController {
    private final NhomService service;

    public DangKyDeTaiRestController(NhomService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<DangKyDeTaiResponse>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.pending(Paging.request(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody DangKyDeTaiRequest body) {
        service.register(body.group(), body.topic());
    }

    @PatchMapping("/{id}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable String id) {
        service.withdraw(id);
    }

    @PatchMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable String id, @Valid @RequestBody DuyetRequest body) {
        service.review(id, body.accept(), body.reason() == null ? "" : body.reason());
    }
}
