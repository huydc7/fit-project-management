package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.DotDangKyForm;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.exception.DatabaseErrorMessages;
import com.example.quanlydetai.security.AccountPrincipal;
import com.example.quanlydetai.service.DotDangKyService;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
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
        add(
                model,
                "dsDot",
                service.search(
                        (String) model.getAttribute("q"),
                        (Pageable) model.getAttribute("pageRequest")));
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

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", service.editForm(id));
        model.addAttribute("editingId", id);
        return danhSach(model);
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("form") DotDangKyForm f,
            BindingResult errors,
            @AuthenticationPrincipal AccountPrincipal user,
            Model model,
            RedirectAttributes flash) {
        model.addAttribute("editingId", id);
        if (errors.hasErrors()) return danhSach(model);
        return com.example.quanlydetai.controller.support.FormFeedback.formAction(
                model,
                flash,
                "/dean/periods",
                () -> service.update(id, f, user.getMaGV()),
                () -> danhSach(model));
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable String id,
            @AuthenticationPrincipal AccountPrincipal user,
            RedirectAttributes flash) {
        return com.example.quanlydetai.controller.support.FormFeedback.perform(
                "/dean/periods", flash, () -> service.delete(id, user.getMaGV()));
    }
}
