package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.DiemRequest;
import com.example.quanlydetai.dto.KetQuaResponse;
import com.example.quanlydetai.dto.PhieuChamDiemResponse;
import com.example.quanlydetai.dto.TongHopDiemRequest;
import com.example.quanlydetai.service.DiemService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<PhieuChamDiemResponse>> tasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.tasks(Paging.request(page, size)));
    }

    @GetMapping("/results/mine")
    public ResponseEntity<List<KetQuaResponse>> mine(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.mine(period, Paging.request(page, size)));
    }

    @GetMapping("/results")
    public ResponseEntity<List<KetQuaResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.allResults(Paging.request(page, size)));
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
