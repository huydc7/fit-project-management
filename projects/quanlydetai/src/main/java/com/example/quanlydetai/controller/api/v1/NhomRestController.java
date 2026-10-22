package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.MoiThanhVienRequest;
import com.example.quanlydetai.dto.NhomRequest;
import com.example.quanlydetai.dto.NhomResponse;
import com.example.quanlydetai.service.NhomService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<NhomResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.mine(Paging.request(page, size)));
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
