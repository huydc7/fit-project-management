package com.example.quanlydetai.controller.api.v1;

import static com.example.quanlydetai.controller.support.PageResponses.from;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.DeTaiResponse;
import com.example.quanlydetai.dto.DuyetRequest;
import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.Paging;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class DeTaiRestController {
    private final DeTaiService service;

    public DeTaiRestController(DeTaiService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DeTaiResponse>> available(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(
                service.available(
                        new TopicSearch(q, department, period, type, state),
                        Paging.request(page, size)));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<DeTaiResponse>> mine(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(
                service.mine(
                        new TopicSearch(q, department, period, type, state),
                        Paging.request(page, size)));
    }

    @GetMapping("/management")
    public ResponseEntity<List<DeTaiResponse>> all(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(
                service.all(
                        new TopicSearch(q, department, period, type, state),
                        Paging.request(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody DeTaiRequest body) {
        service.create(body);
    }

    @PatchMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable String id, @Valid @RequestBody DuyetRequest body) {
        service.review(id, body.accept());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable String id,
            @Valid @RequestBody com.example.quanlydetai.dto.NoiDungDeTaiRequest body) {
        service.updateContent(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deleteDraft(id);
    }

    @GetMapping("/department")
    public ResponseEntity<List<DeTaiResponse>> department(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return from(
                service.departmentTopics(
                        new TopicSearch(q, "", period, type, state), Paging.request(page, size)));
    }

    @PatchMapping("/{id}/department-review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void departmentReview(
            @PathVariable String id,
            @Valid @RequestBody com.example.quanlydetai.dto.DepartmentReviewRequest body) {
        service.reviewDepartment(id, body.accept(), body.supervisors());
    }
}
