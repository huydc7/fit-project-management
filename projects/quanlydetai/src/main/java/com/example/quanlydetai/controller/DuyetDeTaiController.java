package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;

import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.DiemService;
import com.example.quanlydetai.service.HoiDongService;
import com.example.quanlydetai.service.NhomService;

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
        m.addAttribute(
                "topics",
                DeTaiService.filter(
                        topics.all(),
                        (String) m.getAttribute("q"),
                        (String) m.getAttribute("departmentFilter"),
                        (String) m.getAttribute("periodFilter"),
                        (String) m.getAttribute("typeFilter"),
                        (String) m.getAttribute("stateFilter")));
        m.addAttribute("registrations", groups.pending());
        m.addAttribute("results", scores.allResults());
        m.addAttribute("councils", councils.all());
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
        m.addAttribute("q", q);
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("roleFilter", role);
        m.addAttribute("typeFilter", type);
        m.addAttribute("stateFilter", state);
    }
}
