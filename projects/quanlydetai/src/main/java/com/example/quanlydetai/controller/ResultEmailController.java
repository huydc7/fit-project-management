package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.perform;

import com.example.quanlydetai.notification.ResultEmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dean/result-emails")
public class ResultEmailController {
    private final ResultEmailService emails;
    private final boolean enabled;

    public ResultEmailController(
            ResultEmailService emails, @Value("${app.email.enabled:false}") boolean enabled) {
        this.emails = emails;
        this.enabled = enabled;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        var results = emails.search(q, state, page, size);
        model.addAttribute("emails", results.content());
        model.addAttribute("emailPage", results);
        model.addAttribute("q", q);
        model.addAttribute("selectedState", state.strip().toUpperCase(java.util.Locale.ROOT));
        model.addAttribute("deliveryEnabled", enabled);
        return "truongkhoa/result-emails";
    }

    @PostMapping("/{id}/retry")
    public String retry(@PathVariable String id, RedirectAttributes flash) {
        return perform("/dean/result-emails", flash, () -> emails.retry(id));
    }
}
