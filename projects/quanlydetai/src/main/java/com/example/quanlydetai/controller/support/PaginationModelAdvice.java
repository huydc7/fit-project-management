package com.example.quanlydetai.controller.support;

import com.example.quanlydetai.service.Paging;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(annotations = Controller.class)
public class PaginationModelAdvice {
    @ModelAttribute
    public void paging(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "0") int registrationsPage,
            @RequestParam(defaultValue = "0") int resultsPage,
            @RequestParam(defaultValue = "0") int invitationsPage,
            Model model) {
        model.addAttribute("pageRequest", Paging.request(page, size));
        model.addAttribute("registrationPageRequest", Paging.request(registrationsPage, size));
        model.addAttribute("resultPageRequest", Paging.request(resultsPage, size));
        model.addAttribute("invitationPageRequest", Paging.request(invitationsPage, size));
        model.addAttribute("invitationPageIndex", invitationsPage);
        model.addAttribute("pageIndex", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("registrationPageIndex", registrationsPage);
        model.addAttribute("resultPageIndex", resultsPage);
    }
}
