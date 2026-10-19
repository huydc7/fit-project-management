package com.example.quanlydetai.controller.api.v1;

import com.example.quanlydetai.notification.ResultEmailService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/result-emails")
public class ResultEmailRestController {
    private final ResultEmailService emails;

    public ResultEmailRestController(ResultEmailService emails) {
        this.emails = emails;
    }

    @GetMapping
    public ResultEmailService.StatusPage list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return emails.search(q, state, page, size);
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retry(@PathVariable String id) {
        emails.retry(id);
    }
}
