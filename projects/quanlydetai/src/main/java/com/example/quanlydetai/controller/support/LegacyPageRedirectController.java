package com.example.quanlydetai.controller.support;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;
import java.util.Map;

/** Keep existing page bookmarks usable; forms and APIs use English URLs exclusively. */
@Controller
public class LegacyPageRedirectController {
    private static final Map<String, String> PAGES =
            Map.ofEntries(
                    Map.entry("/thong-bao", "/announcements"),
                    Map.entry("/tai-khoan/mat-khau", "/accounts/password"),
                    Map.entry("/admin/tai-khoan", "/admin/accounts"),
                    Map.entry("/sv/de-tai", "/student/topics"),
                    Map.entry("/de-tai", "/topics"),
                    Map.entry("/sv/nhom", "/student/groups"),
                    Map.entry("/sv/bao-cao", "/student/reports"),
                    Map.entry("/sv/ket-qua", "/student/results"),
                    Map.entry("/gv/de-tai", "/lecturer/topics"),
                    Map.entry("/gv/bao-cao", "/lecturer/reports"),
                    Map.entry("/gv/cham-diem", "/lecturer/grading"),
                    Map.entry("/truong-khoa/dot", "/dean/periods"),
                    Map.entry("/truong-khoa/duyet", "/dean/approvals"),
                    Map.entry("/truong-khoa/hoi-dong", "/dean/councils"));

    @GetMapping({
        "/thong-bao",
        "/tai-khoan/mat-khau",
        "/admin/tai-khoan",
        "/sv/de-tai",
        "/de-tai",
        "/sv/nhom",
        "/sv/bao-cao",
        "/sv/ket-qua",
        "/gv/de-tai",
        "/gv/bao-cao",
        "/gv/cham-diem",
        "/truong-khoa/dot",
        "/truong-khoa/duyet",
        "/truong-khoa/hoi-dong"
    })
    public ResponseEntity<Void> redirect(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(request.getContextPath() + PAGES.get(path)))
                .build();
    }
}
