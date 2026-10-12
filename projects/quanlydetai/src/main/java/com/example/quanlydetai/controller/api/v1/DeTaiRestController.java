package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.DeTaiResponse;
import com.example.quanlydetai.dto.DuyetRequest;
import com.example.quanlydetai.service.DeTaiService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class DeTaiRestController {
    private final DeTaiService service;

    public DeTaiRestController(DeTaiService service) {
        this.service = service;
    }

    @GetMapping
    public List<DeTaiResponse> available() {
        return service.available();
    }

    @GetMapping("/mine")
    public List<DeTaiResponse> mine() {
        return service.mine();
    }

    @GetMapping("/management")
    public List<DeTaiResponse> all() {
        return service.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody DeTaiRequest body) {
        service.create(body);
    }

    @PatchMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable String id, @Valid @RequestBody DuyetRequest body) {
        service.review(id, body.accept());
    }
}
