package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.DiemService;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
public class DuyetDeTaiController {
    private final DeTaiService topics;
    private final DiemService scores;

    public DuyetDeTaiController(DeTaiService topics, DiemService scores) {
        this.topics = topics;
        this.scores = scores;
    }

    @GetMapping("/dean/approvals")
    public String approvals(Model m) {
        options(m);
        var topicPage =
                topics.pendingApproval(
                        (TopicSearch) m.getAttribute("topicSearch"),
                        (Pageable) m.getAttribute("pageRequest"));
        add(m, "topics", topicPage);
        add(m, "results", scores.pendingPublication((TopicSearch) m.getAttribute("topicSearch"),
                (Pageable) m.getAttribute("resultPageRequest")));
        m.addAttribute("approvalQueue", true);
        return "truongkhoa/approvals";
    }

    private void options(Model m) {
        m.addAttribute("periods", topics.periods());
        m.addAttribute("departments", topics.departments());
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
        m.addAttribute("topicSearch", new TopicSearch(q, department, period, type, ""));
        m.addAttribute("q", q);
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("roleFilter", role);
        m.addAttribute("typeFilter", type);
        m.addAttribute("stateFilter", "");
    }
}
