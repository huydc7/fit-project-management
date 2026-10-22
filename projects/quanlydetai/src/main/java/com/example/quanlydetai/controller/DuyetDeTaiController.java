package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.DiemService;
import com.example.quanlydetai.service.HoiDongService;
import com.example.quanlydetai.service.NhomService;

import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class DuyetDeTaiController {
    private final DeTaiService topics;
    private final NhomService groups;
    private final DiemService scores;
    private final HoiDongService councils;

    public DuyetDeTaiController(
            DeTaiService topics, NhomService groups, DiemService scores, HoiDongService councils) {
        this.topics = topics;
        this.groups = groups;
        this.scores = scores;
        this.councils = councils;
    }

    @GetMapping("/dean/approvals")
    public String approvals(Model m) {
        options(m);
        var topicPage =
                topics.all(
                        (TopicSearch) m.getAttribute("topicSearch"),
                        (Pageable) m.getAttribute("pageRequest"));
        add(m, "topics", topicPage);
        add(
                m,
                "registrations",
                groups.pending((Pageable) m.getAttribute("registrationPageRequest")));
        add(m, "results", scores.allResults((Pageable) m.getAttribute("resultPageRequest")));
        m.addAttribute(
                "councils",
                councils.optionsForPeriods(
                        topicPage.stream().map(t -> t.periodId()).distinct().toList()));
        return "truongkhoa/approvals";
    }

    private void options(Model m) {
        m.addAttribute("periods", topics.periods());
        m.addAttribute("departments", topics.departments());
        m.addAttribute("teachers", topics.teachers());
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
}
