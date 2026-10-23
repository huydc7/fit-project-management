package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.service.HoiDongService;
import com.example.quanlydetai.service.ReferenceDataService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
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
    public String councilPage(@RequestParam(defaultValue = "") String topic, Model m) {
        var targets = councils.topicOptions();
        if (m.containsAttribute("editingId")) {
            var form = (HoiDongRequest) m.getAttribute("form");
            targets = targets.stream().filter(t -> t.period().equals(form.period())).toList();
        }
        m.addAttribute("topicOptions", targets);
        m.addAttribute("selectedTopic", topic);
        var selected = targets.stream().filter(t -> t.id().equals(topic)).findFirst();
        m.addAttribute("selectedTopicLabel", selected.map(t -> t.label()).orElse(topic));
        if (!topic.isBlank() && selected.isEmpty()) {
            m.addAttribute("loi", "Đề tài không còn khả dụng để lập hội đồng: cần có nhóm được duyệt và chưa có điểm.");
            m.addAttribute("selectedTopic", "");
        }
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new HoiDongRequest(selected.map(t -> t.period()).orElse(null), null, null, List.of()));
        m.addAttribute("periods", referenceData.periods());
        m.addAttribute("teachers", selected.isPresent() ? councils.candidates(topic) : List.of());
        add(m, "councils", councils.all((Pageable) m.getAttribute("pageRequest")));
        return "truongkhoa/councils";
    }

    @PostMapping("/dean/councils")
    public String createCouncil(
            @Valid @ModelAttribute("form") HoiDongRequest f,
            BindingResult errors,
            Model m,
            @RequestParam(defaultValue = "") String topic,
            RedirectAttributes flash) {
        if (topic.isBlank()) {
            m.addAttribute("loi", "Hãy chọn đề tài trước khi lập hội đồng.");
            return councilPage(topic, m);
        }
        if (errors.hasErrors()) return councilPage(topic, m);
        return formAction(
                m, flash, "/dean/councils", () -> councils.createForTopic(topic, f), () -> councilPage(topic, m));
    }

    @PostMapping("/dean/councils/{id}/finalize")
    public String finalizeCouncil(@PathVariable String id, @RequestParam(defaultValue = "") String topic, RedirectAttributes flash) {
        return perform("/dean/councils", flash, () -> councils.finalizeForTopic(id, topic));
    }

    @GetMapping("/dean/councils/{id}/edit")
    public String edit(@PathVariable String id, @RequestParam(defaultValue = "") String topic, Model m) {
        m.addAttribute("form", councils.editForm(id));
        m.addAttribute("editingId", id);
        return councilPage(topic, m);
    }

    @PostMapping("/dean/councils/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("form") HoiDongRequest f,
            BindingResult errors,
            Model m,
            @RequestParam(defaultValue = "") String topic,
            RedirectAttributes flash) {
        m.addAttribute("editingId", id);
        String context = topic;
        if (errors.hasErrors()) return councilPage(context, m);
        return formAction(
                m, flash, "/dean/councils", () -> councils.updateForTopic(id, topic, f), () -> councilPage(context, m));
    }

    @PostMapping("/dean/councils/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes flash) {
        return perform("/dean/councils", flash, () -> councils.deleteDraft(id));
    }
}
