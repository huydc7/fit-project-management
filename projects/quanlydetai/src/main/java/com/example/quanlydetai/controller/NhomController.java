package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.CapNhatNhomRequest;
import com.example.quanlydetai.dto.NhomRequest;
import com.example.quanlydetai.service.NhomService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class NhomController {
    private final NhomService groups;

    public NhomController(NhomService groups) {
        this.groups = groups;
    }

    @GetMapping("/student/groups")
    public String studentGroups(Model m) {
        if (!m.containsAttribute("form")) m.addAttribute("form", new NhomRequest(null, null));
        m.addAttribute("periods", groups.periods());
        add(m, "groups", groups.mine((Pageable) m.getAttribute("pageRequest")));
        add(
                m,
                "invitations",
                groups.invitations((Pageable) m.getAttribute("invitationPageRequest")));
        return "sv/groups";
    }

    @PostMapping("/student/groups")
    public String createGroup(
            @Valid @ModelAttribute("form") NhomRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return studentGroups(m);
        return formAction(
                m, flash, "/student/groups", () -> groups.create(f), () -> studentGroups(m));
    }

    @PostMapping("/student/groups/{id}/invitations")
    public String invite(
            @PathVariable String id, @RequestParam String student, RedirectAttributes flash) {
        return perform("/student/groups", flash, () -> groups.invite(id, student));
    }

    @PostMapping("/student/invitations/{id}")
    public String answer(
            @PathVariable String id, @RequestParam boolean accept, RedirectAttributes flash) {
        return perform("/student/groups", flash, () -> groups.answer(id, accept));
    }

    @PostMapping("/student/groups/{id}/name")
    public String renameGroup(
            @PathVariable String id,
            @Valid @ModelAttribute("groupEditForm") CapNhatNhomRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        m.addAttribute("editingGroupId", id);
        if (errors.hasErrors()) {
            m.addAttribute("groupEditError", errors.getFieldError("name").getDefaultMessage());
            return studentGroups(m);
        }
        var page = (Pageable) m.getAttribute("pageRequest");
        flash.addAttribute("page", page.getPageNumber());
        flash.addAttribute("size", page.getPageSize());
        return formAction(m, flash, "/student/groups", () -> groups.rename(id, f), () -> studentGroups(m));
    }

    @PostMapping("/student/groups/{id}/members/remove")
    public String remove(
            @PathVariable String id, @RequestParam String student, RedirectAttributes flash) {
        return perform("/student/groups", flash, () -> groups.removeMember(id, student));
    }

    @PostMapping("/student/registrations")
    public String register(
            @RequestParam String group, @RequestParam String topic, RedirectAttributes flash) {
        return perform("/student/groups", flash, () -> groups.register(group, topic));
    }

    @PostMapping("/student/registrations/{id}/withdraw")
    public String withdraw(@PathVariable String id, RedirectAttributes flash) {
        return perform("/student/groups", flash, () -> groups.withdraw(id));
    }

    @GetMapping("/lecturer/registrations")
    public String pendingRegistrations(Model m) {
        add(m, "registrations", groups.pending((Pageable) m.getAttribute("registrationPageRequest")));
        return "gv/registrations";
    }

    @PostMapping("/lecturer/registrations/{id}/review")
    public String reviewRegistration(
            @PathVariable String id,
            @RequestParam boolean accept,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(defaultValue = "0") int registrationsPage,
            @RequestParam(defaultValue = "25") int size,
            RedirectAttributes flash) {
        String result = perform("/lecturer/registrations", flash, () -> groups.review(id, accept, reason));
        if (flash.getFlashAttributes().containsKey("loi")) {
            flash.addFlashAttribute("registrationErrorId", id);
            flash.addFlashAttribute("registrationReason", reason);
            flash.addAttribute("registrationsPage", registrationsPage);
            flash.addAttribute("size", size);
            return result + "#registration-" + id;
        }
        return result;
    }
}
