package com.example.quanlydetai.controller;

import com.example.quanlydetai.dto.DotDangKyForm;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.exception.DatabaseErrorMessages;
import com.example.quanlydetai.security.AccountPrincipal;
import com.example.quanlydetai.service.DotDangKyService;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dean/periods")
public class DotDangKyController {
    private final DotDangKyService service;

    public DotDangKyController(DotDangKyService service) {
        this.service = service;
    }

    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("dsDot", service.list());
        if (!model.containsAttribute("form")) model.addAttribute("form", new DotDangKyForm());
        return "truongkhoa/dot-list";
    }

    @PostMapping
    public String taoDot(
            @Valid @ModelAttribute("form") DotDangKyForm form,
            BindingResult errors,
            @AuthenticationPrincipal AccountPrincipal user,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute(
                    "loi", errors.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList());
            return danhSach(model);
        }
        try {
            service.create(form, user.getMaGV());
            redirect.addFlashAttribute("thanhCong", "Đã tạo đợt đăng ký.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("loi", List.of(ex.getMessage()));
            redirect.addFlashAttribute("form", form);
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute(
                    "loi", List.of(DatabaseErrorMessages.extractRootCauseMessage(ex)));
            redirect.addFlashAttribute("form", form);
        }
        return "redirect:/dean/periods";
    }
}
