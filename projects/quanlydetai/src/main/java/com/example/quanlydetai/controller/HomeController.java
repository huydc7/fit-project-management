package com.example.quanlydetai.controller;

import com.example.quanlydetai.service.ThongBaoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ThongBaoService notices;

    public HomeController(ThongBaoService notices) {
        this.notices = notices;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("tieuDe", "Hệ thống quản lý đề tài sinh viên");
        model.addAttribute("recentNotices", notices.recent());
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
