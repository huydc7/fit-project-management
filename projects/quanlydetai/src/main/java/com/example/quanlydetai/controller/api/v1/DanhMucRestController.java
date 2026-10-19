package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.service.ReferenceDataService;

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
    public List<LuaChonResponse> departments(@RequestParam(defaultValue = "") String q) {
        return service.searchDepartments(q);
    }

    @GetMapping("/lecturers")
    public List<LuaChonResponse> teachers() {
        return service.teachers();
    }
}
