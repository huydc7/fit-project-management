package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.service.HoiDongService;
import com.example.quanlydetai.service.ReferenceDataService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class HoiDongController {
    private final HoiDongService councils;
    private final ReferenceDataService referenceData;

    public HoiDongController(HoiDongService councils, ReferenceDataService referenceData) {
        this.councils = councils;
        this.referenceData = referenceData;
    }

    @PostMapping("/dean/topics/{id}/reviewer")
    public String assignReviewer(
            @PathVariable String id, @RequestParam String teacher, RedirectAttributes flash) {
        return perform("/dean/approvals", flash, () -> councils.assignReviewer(id, teacher));
    }

    @PostMapping("/dean/topics/{id}/councils")
    public String assignCouncil(
            @PathVariable String id, @RequestParam String council, RedirectAttributes flash) {
        return perform("/dean/approvals", flash, () -> councils.assignCouncil(id, council));
    }

    @GetMapping("/dean/councils")
    public String councilPage(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new HoiDongRequest(null, null, null, List.of()));
        options(m);
        m.addAttribute("councils", councils.all());
        return "truongkhoa/councils";
    }

    @PostMapping("/dean/councils")
    public String createCouncil(
            @Valid @ModelAttribute("form") HoiDongRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return councilPage(m);
        return formAction(
                m, flash, "/dean/councils", () -> councils.create(f), () -> councilPage(m));
    }

    @PostMapping("/dean/councils/{id}/finalize")
    public String finalizeCouncil(@PathVariable String id, RedirectAttributes flash) {
        return perform("/dean/councils", flash, () -> councils.finalizeCouncil(id));
    }

    private void options(Model m) {
        m.addAttribute("periods", referenceData.periods());
        m.addAttribute("departments", referenceData.departments());
        m.addAttribute("teachers", referenceData.teachers());
    }
}
