package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.perform;

import com.example.quanlydetai.service.DeTaiService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/department/topics")
public class TruongBoMonController {
    private final DeTaiService topics;

    public TruongBoMonController(DeTaiService topics) {
        this.topics = topics;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            Model m) {
        m.addAttribute(
                "topics",
                DeTaiService.filter(topics.departmentTopics(), q, department, period, type, state));
        m.addAttribute("q", q);
        m.addAttribute("departments", topics.departments());
        m.addAttribute("periods", topics.periods());
        m.addAttribute("teachers", topics.teachers());
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("typeFilter", type);
        m.addAttribute("stateFilter", state);
        return "gv/department-topics";
    }

    @PostMapping("/{id}/review")
    public String review(
            @PathVariable String id, @RequestParam boolean accept,
            @RequestParam(required = false) java.util.List<String> supervisors, RedirectAttributes flash) {
        return perform("/department/topics", flash, () -> topics.reviewDepartment(id, accept, supervisors));
    }
}
