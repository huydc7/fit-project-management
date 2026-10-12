package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.BaoCaoResponse;
import com.example.quanlydetai.service.BaoCaoService;

import org.springframework.http.HttpStatus;
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
    public List<BaoCaoResponse> mine() {
        return service.mine();
    }

    @GetMapping("/assigned")
    public List<BaoCaoResponse> assigned() {
        return service.assigned();
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
