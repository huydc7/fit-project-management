package com.example.quanlydetai.controller.support;

import com.example.quanlydetai.security.AccountPrincipal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class CurrentAccountModelAdvice {
    @ModelAttribute("accountDisplayName")
    public String displayName(@AuthenticationPrincipal AccountPrincipal user) {
        return user == null ? "Tài khoản" : user.getDisplayName();
    }

    @ModelAttribute("topicManagementPath")
    public String topicManagementPath(@AuthenticationPrincipal AccountPrincipal user) {
        if (user != null) {
            var roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            if (roles.contains("ROLE_TRUONGKHOA")) return "/dean/topics";
            if (roles.contains("ROLE_TRUONGBOMON")) return "/department/topic-management";
        }
        return "/lecturer/topic-management";
    }
}
