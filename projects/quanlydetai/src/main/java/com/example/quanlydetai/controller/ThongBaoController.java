package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.service.ThongBaoService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class ThongBaoController {
    private final ThongBaoService notices;

    public ThongBaoController(ThongBaoService notices) {
        this.notices = notices;
    }

    @GetMapping("/announcements")
    public String noticePage(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new ThongBaoRequest(null, null, "Khoa"));
        add(
                m,
                "notices",
                notices.search(
                        (String) m.getAttribute("q"), (Pageable) m.getAttribute("pageRequest")));
        return "notices";
    }

    @PostMapping("/announcements")
    public String createNotice(
            @Valid @ModelAttribute("form") ThongBaoRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return noticePage(m);
        return formAction(m, flash, "/announcements", () -> notices.create(f), () -> noticePage(m));
    }

    @ModelAttribute
    void filters(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String role,
            Model m) {
        m.addAttribute("q", q);
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("roleFilter", role);
    }

    @PostMapping("/announcements/{id}/edit")
    public String edit(
            @PathVariable String id,
            @Valid @ModelAttribute ThongBaoRequest f,
            BindingResult errors,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("loi", "Thông báo chưa hợp lệ.");
            return "redirect:/announcements";
        }
        return perform("/announcements", flash, () -> notices.update(id, f));
    }

    @PostMapping("/announcements/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes flash) {
        return perform("/announcements", flash, () -> notices.delete(id));
    }
}
