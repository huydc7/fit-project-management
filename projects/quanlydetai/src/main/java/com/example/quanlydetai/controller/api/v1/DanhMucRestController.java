package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.service.Paging;
import com.example.quanlydetai.service.ReferenceDataService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class DanhMucRestController {
    private final ReferenceDataService service;

    public DanhMucRestController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/periods")
    public List<LuaChonResponse> periods() {
        return service.periods();
    }

    @GetMapping("/departments")
    public ResponseEntity<List<LuaChonResponse>> departments(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(service.searchDepartments(q, Paging.request(page, size)));
    }

    @GetMapping("/lecturers")
    public List<LuaChonResponse> teachers() {
        return service.teachers();
    }
}
