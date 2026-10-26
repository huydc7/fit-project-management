package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.service.BaoCaoService;
import com.example.quanlydetai.service.NhomService;

import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class BaoCaoController {
    private final BaoCaoService reports;
    private final NhomService groups;

    public BaoCaoController(BaoCaoService reports, NhomService groups) {
        this.reports = reports;
        this.groups = groups;
    }

    @GetMapping("/student/reports")
    public String studentReports(Model m) {
        m.addAttribute("groups", groups.reportOptions());
        add(m, "reports", reports.mine((Pageable) m.getAttribute("pageRequest")));
        m.addAttribute("token", UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        return "sv/reports";
    }

    @PostMapping("/student/reports")
    public String submitReport(
            @RequestParam String group,
            @RequestParam String token,
            @RequestParam MultipartFile file,
            RedirectAttributes flash) {

        return perform("/student/reports", flash, () -> reports.submit(group, token, file));
    }

    @GetMapping("/reports/{id}/download")
    public ResponseEntity<?> download(@PathVariable String id) {
        return com.example.quanlydetai.controller.support.ReportDownloadResponse.from(
                reports.download(id));
    }

    @GetMapping("/lecturer/reports")
    public String assignedReports(Model m) {
        add(m, "reports", reports.assigned((Pageable) m.getAttribute("pageRequest")));
        return "gv/reports";
    }
}
