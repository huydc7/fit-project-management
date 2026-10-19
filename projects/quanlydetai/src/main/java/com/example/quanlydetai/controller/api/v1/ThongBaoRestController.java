package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.dto.ThongBaoResponse;
import com.example.quanlydetai.service.ThongBaoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
    public List<ThongBaoResponse> list(@RequestParam(defaultValue = "") String q) {
        return service.search(q);
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
