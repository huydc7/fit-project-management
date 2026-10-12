package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.NhomService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class DeTaiController {
    private final DeTaiService topics;
    private final NhomService groups;

    public DeTaiController(DeTaiService topics, NhomService groups) {
        this.topics = topics;
        this.groups = groups;
    }

    @GetMapping("/lecturer/topics")
    public String teacherTopics(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new DeTaiRequest(null, null, null, null, List.of()));
        options(m);
        m.addAttribute("topics", topics.mine());
        return "gv/topics";
    }

    @PostMapping("/lecturer/topics")
    public String createTopic(
            @Valid @ModelAttribute("form") DeTaiRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return teacherTopics(m);
        return formAction(
                m, flash, "/lecturer/topics", () -> topics.create(f), () -> teacherTopics(m));
    }

    @PostMapping("/dean/topics/{id}/review")
    public String reviewTopic(
            @PathVariable String id, @RequestParam boolean accept, RedirectAttributes flash) {
        return perform("/dean/approvals", flash, () -> topics.review(id, accept));
    }

    private void options(Model m) {
        m.addAttribute("periods", topics.periods());
        m.addAttribute("departments", topics.departments());
        m.addAttribute("teachers", topics.teachers());
    }

    @GetMapping({"/student/topics", "/topics"})
    public String danhSach(Model model) {
        model.addAttribute("topics", topics.available());
        model.addAttribute("groups", groups.mine());
        return "sv/de-tai-list";
    }
}
