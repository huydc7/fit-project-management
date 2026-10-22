package com.example.quanlydetai.service;

import com.example.quanlydetai.exception.BusinessException;

import org.springframework.data.domain.*;

public final class Paging {
    private Paging() {}

    public static Pageable defaults() {
        return request(0, 25);
    }

    public static Pageable request(int page, int size) {
        validate(page, size);
        return PageRequest.of(page, size);
    }

    public static void check(Pageable page) {
        if (page == null || page.isUnpaged())
            throw new BusinessException("Danh sách cần được phân trang.");
        validate(page.getPageNumber(), page.getPageSize());
    }

    private static void validate(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new BusinessException("Trang cần từ 0 trở lên; mỗi trang từ 1 đến 100 dòng.");
    }
}
