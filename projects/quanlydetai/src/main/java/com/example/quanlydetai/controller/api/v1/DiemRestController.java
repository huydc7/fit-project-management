package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.DiemRequest;
import com.example.quanlydetai.dto.KetQuaResponse;
import com.example.quanlydetai.dto.PhieuChamDiemResponse;
import com.example.quanlydetai.dto.TongHopDiemRequest;
import com.example.quanlydetai.service.DiemService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
public class DiemRestController {
    private final DiemService service;

    public DiemRestController(DiemService service) {
        this.service = service;
    }

    @GetMapping("/ballots")
    public List<PhieuChamDiemResponse> tasks() {
        return service.tasks();
    }

    @GetMapping("/results/mine")
    public List<KetQuaResponse> mine(@RequestParam(required = false) String period) {
        return service.mine(period);
    }

    @GetMapping("/results")
    public List<KetQuaResponse> all() {
        return service.allResults();
    }

    @PutMapping("/ballots")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@Valid @RequestBody DiemRequest body) {
        service.submit(body);
    }

    @PostMapping("/topics/{id}/aggregate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aggregate(@PathVariable String id, @Valid @RequestBody TongHopDiemRequest body) {
        service.aggregate(id, body.comment());
    }

    @PatchMapping("/topics/{id}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@PathVariable String id) {
        service.publish(id);
    }
}
