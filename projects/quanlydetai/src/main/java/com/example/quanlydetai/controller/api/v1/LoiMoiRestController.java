package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.LoiMoiResponse;
import com.example.quanlydetai.dto.TraLoiMoiRequest;
import com.example.quanlydetai.service.NhomService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
    public List<LoiMoiResponse> mine() {
        return service.invitations();
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@PathVariable String id, @Valid @RequestBody TraLoiMoiRequest body) {
        service.answer(id, body.accept());
    }
}
