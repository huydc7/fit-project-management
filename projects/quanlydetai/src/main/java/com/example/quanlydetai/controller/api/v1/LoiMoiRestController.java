package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.LoiMoiResponse;
import com.example.quanlydetai.dto.TraLoiMoiRequest;
import com.example.quanlydetai.service.NhomService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@PreAuthorize("hasRole('SV')")
public class LoiMoiRestController {
    private final NhomService service;

    public LoiMoiRestController(NhomService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LoiMoiResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.invitations(Paging.request(page, size)));
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@PathVariable String id, @Valid @RequestBody TraLoiMoiRequest body) {
        service.answer(id, body.accept());
    }
}
