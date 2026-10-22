package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.DiemRequest;
import com.example.quanlydetai.service.DiemService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class DiemController {
    private final DiemService scores;

    public DiemController(DiemService scores) {
        this.scores = scores;
    }

    @GetMapping("/student/results")
    public String results(@RequestParam(required = false) String period, Model m) {
        add(m, "results", scores.mine(period, (Pageable) m.getAttribute("pageRequest")));
        m.addAttribute("periods", scores.myResultPeriods());
        m.addAttribute("selectedPeriod", period == null ? "" : period.strip());
        return "sv/results";
    }

    @GetMapping("/lecturer/grading")
    public String grading(Model m) {
        add(m, "tasks", scores.tasks((Pageable) m.getAttribute("pageRequest")));
        return "gv/scores";
    }

    @PostMapping("/lecturer/grading")
    public String submitScore(
            @Valid @ModelAttribute("scoreForm") DiemRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            m.addAttribute(
                    "loi",
                    "Điểm cần từ 0 đến 10, tối đa hai chữ số thập phân; nhận xét không được"
                            + " trống.");
            return grading(m);
        }
        return formAction(m, flash, "/lecturer/grading", () -> scores.submit(f), () -> grading(m));
    }

    @PostMapping("/lecturer/topics/{id}/aggregate")
    public String aggregate(
            @PathVariable String id, @RequestParam String comment, RedirectAttributes flash) {
        return perform("/lecturer/grading", flash, () -> scores.aggregate(id, comment));
    }

    @PostMapping("/dean/topics/{id}/publish")
    public String publish(@PathVariable String id, RedirectAttributes flash) {
        return perform("/dean/approvals", flash, () -> scores.publish(id));
    }
}
