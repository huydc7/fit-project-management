package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.service.ThongBaoService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

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
        m.addAttribute("notices", notices.list());
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
}
