package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.BaoCaoResponse;
import com.example.quanlydetai.service.BaoCaoService;
import com.example.quanlydetai.service.Paging;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class BaoCaoRestController {
    private final BaoCaoService service;

    public BaoCaoRestController(BaoCaoService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BaoCaoResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.mine(Paging.request(page, size)));
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<BaoCaoResponse>> assigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.assigned(Paging.request(page, size)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(
            @RequestParam String group,
            @RequestParam String token,
            @RequestParam org.springframework.web.multipart.MultipartFile file) {
        service.submit(group, token, file);
    }

    @GetMapping("/{id}/download")
    public org.springframework.http.ResponseEntity<?> download(@PathVariable String id) {
        return com.example.quanlydetai.controller.support.ReportDownloadResponse.from(
                service.download(id));
    }
}
