package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.dto.ThongBaoResponse;
import com.example.quanlydetai.service.Paging;
import com.example.quanlydetai.service.ThongBaoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/announcements")
public class ThongBaoRestController {
    private final ThongBaoService service;

    public ThongBaoRestController(ThongBaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ThongBaoResponse>> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.search(q, Paging.request(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody ThongBaoRequest body) {
        service.create(body);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody ThongBaoRequest body) {
        service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
