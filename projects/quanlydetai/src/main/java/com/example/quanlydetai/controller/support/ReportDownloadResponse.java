package com.example.quanlydetai.controller.support;

import com.example.quanlydetai.storage.ReportStorageService;

import org.springframework.http.*;

public final class ReportDownloadResponse {
    private ReportDownloadResponse() {}

    public static ResponseEntity<?> from(ReportStorageService.Download download) {
        if (download.redirect() != null)
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(download.redirect())
                    .cacheControl(CacheControl.noStore())
                    .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.filename())
                                .build()
                                .toString())
                .body(download.resource());
    }
}
