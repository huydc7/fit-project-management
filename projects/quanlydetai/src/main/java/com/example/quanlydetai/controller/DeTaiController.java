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
        m.addAttribute(
                "topics",
                DeTaiService.filter(
                        topics.mine(),
                        (String) m.getAttribute("q"),
                        (String) m.getAttribute("departmentFilter"),
                        (String) m.getAttribute("periodFilter"),
                        (String) m.getAttribute("typeFilter"),
                        (String) m.getAttribute("stateFilter")));
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
        options(model);
        model.addAttribute(
                "topics",
                DeTaiService.filter(
                        topics.available(),
                        (String) model.getAttribute("q"),
                        (String) model.getAttribute("departmentFilter"),
                        (String) model.getAttribute("periodFilter"),
                        (String) model.getAttribute("typeFilter"),
                        (String) model.getAttribute("stateFilter")));
        model.addAttribute("groups", groups.mine());
        return "sv/de-tai-list";
    }

    @ModelAttribute
    void filters(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            Model m) {
        m.addAttribute("q", q);
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("roleFilter", role);
        m.addAttribute("typeFilter", type);
        m.addAttribute("stateFilter", state);
    }

    @PostMapping("/lecturer/topics/{id}/edit")
    public String edit(
            @PathVariable String id,
            @Valid @ModelAttribute com.example.quanlydetai.dto.NoiDungDeTaiRequest f,
            BindingResult errors,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("loi", "Tên/nội dung đề tài chưa hợp lệ.");
            return "redirect:/lecturer/topics";
        }
        return perform("/lecturer/topics", flash, () -> topics.updateContent(id, f));
    }

    @PostMapping("/lecturer/topics/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes flash) {
        return perform("/lecturer/topics", flash, () -> topics.deleteDraft(id));
    }
}
