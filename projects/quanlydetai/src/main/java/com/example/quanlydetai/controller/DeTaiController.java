package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.NhomService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


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
            m.addAttribute("form", new DeTaiRequest(null, null, null, null, null));
        options(m);
        add(
                m,
                "topics",
                topics.mine(
                        (TopicSearch) m.getAttribute("topicSearch"),
                        (Pageable) m.getAttribute("pageRequest")));
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
        var topicPage =
                topics.available(
                        (TopicSearch) model.getAttribute("topicSearch"),
                        (Pageable) model.getAttribute("pageRequest"));
        add(model, "topics", topicPage);
        model.addAttribute(
                "groups",
                groups.registrationOptions(
                        topicPage.stream().map(t -> t.periodId()).distinct().toList()));
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
        m.addAttribute("topicSearch", new TopicSearch(q, department, period, type, state));
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
