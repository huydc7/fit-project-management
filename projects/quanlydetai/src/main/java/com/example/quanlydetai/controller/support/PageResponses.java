package com.example.quanlydetai.controller.support;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.List;

public final class PageResponses {
    private PageResponses() {}

    public static <T> ResponseEntity<List<T>> from(Page<T> page) {
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(page.getTotalElements()))
                .header("X-Page", Integer.toString(page.getNumber()))
                .header("X-Page-Size", Integer.toString(page.getSize()))
                .body(page.getContent());
    }

    public static void add(Model model, String name, Page<?> page) {
        model.addAttribute(name, page.getContent());
        model.addAttribute(name + "Page", page);
    }
}
