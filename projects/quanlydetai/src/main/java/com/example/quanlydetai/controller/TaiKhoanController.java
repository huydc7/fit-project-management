package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;

import com.example.quanlydetai.dto.DoiMatKhauRequest;
import com.example.quanlydetai.dto.TaiKhoanRequest;
import com.example.quanlydetai.service.ReferenceDataService;
import com.example.quanlydetai.service.TaiKhoanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class TaiKhoanController {
    private final TaiKhoanService accounts;
    private final ReferenceDataService referenceData;

    public TaiKhoanController(TaiKhoanService accounts, ReferenceDataService referenceData) {
        this.accounts = accounts;
        this.referenceData = referenceData;
    }

    @GetMapping("/admin/accounts")
    public String accountPage(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new TaiKhoanRequest(null, null, "SV", null, null, null, null));
        options(m);
        m.addAttribute("accounts", accounts.list());
        return "admin/accounts";
    }

    @PostMapping("/admin/accounts")
    public String createAccount(
            @Valid @ModelAttribute("form") TaiKhoanRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return accountPage(m);
        return formAction(
                m, flash, "/admin/accounts", () -> accounts.create(f), () -> accountPage(m));
    }

    @PostMapping("/admin/accounts/{id}/status")
    public String accountActive(
            @PathVariable String id, @RequestParam boolean active, RedirectAttributes flash) {
        return perform("/admin/accounts", flash, () -> accounts.setActive(id, active));
    }

    @PostMapping("/admin/accounts/{id}/password")
    public String resetPassword(
            @PathVariable String id, @RequestParam String password, RedirectAttributes flash) {
        return perform("/admin/accounts", flash, () -> accounts.resetPassword(id, password));
    }

    @PostMapping("/admin/departments")
    public String addDepartment(
            @RequestParam String id, @RequestParam String name, RedirectAttributes flash) {
        return perform("/admin/accounts", flash, () -> accounts.addDepartment(id, name));
    }

    @PostMapping("/admin/dean")
    public String dean(
            @RequestParam String teacher,
            @RequestParam boolean appointed,
            RedirectAttributes flash) {
        return perform("/admin/accounts", flash, () -> accounts.dean(teacher, appointed));
    }

    @GetMapping("/accounts/password")
    public String passwordPage(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new DoiMatKhauRequest(null, null, null));
        return "auth/password";
    }

    @PostMapping("/accounts/password")
    public String changePassword(
            @Valid @ModelAttribute("form") DoiMatKhauRequest f,
            BindingResult errors,
            Model m,
            HttpServletRequest request,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return passwordPage(m);
        String result = perform("/accounts/password", flash, () -> accounts.changePassword(f));
        if (flash.getFlashAttributes().containsKey("thanhCong")) {
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            return "redirect:/login?passwordChanged";
        }
        return result;
    }

    private void options(Model m) {
        m.addAttribute("periods", referenceData.periods());
        m.addAttribute("departments", referenceData.departments());
        m.addAttribute("teachers", referenceData.teachers());
    }
}
