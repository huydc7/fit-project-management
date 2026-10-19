package com.example.quanlydetai.service;

import java.text.Normalizer;
import java.util.Locale;

final class SearchText {
    private SearchText() {}

    static boolean matches(String keyword, String... values) {
        String key = normalize(keyword);
        if (key.isEmpty()) return true;
        for (String value : values) if (normalize(value).contains(key)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace('đ', 'd');
    }
}
