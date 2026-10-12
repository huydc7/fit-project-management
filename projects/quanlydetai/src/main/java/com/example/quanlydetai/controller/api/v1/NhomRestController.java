package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.MoiThanhVienRequest;
import com.example.quanlydetai.dto.NhomRequest;
import com.example.quanlydetai.dto.NhomResponse;
import com.example.quanlydetai.service.NhomService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@PreAuthorize("hasRole('SV')")
public class NhomRestController {
    private final NhomService service;

    public NhomRestController(NhomService service) {
        this.service = service;
    }

    @GetMapping
    public List<NhomResponse> mine() {
        return service.mine();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody NhomRequest body) {
        service.create(body);
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public void invite(@PathVariable String id, @Valid @RequestBody MoiThanhVienRequest body) {
        service.invite(id, body.student());
    }

    @DeleteMapping("/{id}/members/{student}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id, @PathVariable String student) {
        service.removeMember(id, student);
    }
}
