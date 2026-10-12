package com.example.quanlydetai.storage;

import com.example.quanlydetai.exception.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

/** Storage adapter only. A business service MUST authorize the actor before calling it. */
@Service
public class SupabaseStorageService {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SupabaseStorageService.class);
    private final String baseUrl;
    private final String bucket;
    private final String apiKey;
    private final RestClient client;
    private final ReportFileValidator validator;

    public record StoredObject(String bucket, String objectKey, long size, String contentType) {}

    @Autowired
    public SupabaseStorageService(
            @Value("${supabase.url:}") String baseUrl,
            @Value("${supabase.key:}") String apiKey,
            @Value("${supabase.bucket:bao-cao-de-tai}") String bucket,
            ReportFileValidator validator) {
        this(baseUrl, apiKey, bucket, validator, httpBuilder());
    }

    SupabaseStorageService(
            String baseUrl,
            String apiKey,
            String bucket,
            ReportFileValidator validator,
            RestClient.Builder builder) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.bucket = bucket;
        this.validator = validator;
        this.client =
                builder.baseUrl(this.baseUrl + "/storage/v1")
                        .defaultHeader("apikey", apiKey)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .build();
    }

    private static RestClient.Builder httpBuilder() {
        var http =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory);
    }

    public StoredObject upload(MultipartFile file) {
        var validated = validator.validate(file);
        configured();
        String objectKey = "reports/" + UUID.randomUUID() + "." + validated.extension();
        try {
            client.post()
                    .uri(
                            "/object/{bucket}/{folder}/{file}",
                            bucket,
                            "reports",
                            objectKey.substring(8))
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(validated.contentType()))
                    .body(validated.bytes())
                    .retrieve()
                    .toBodilessEntity();
            return new StoredObject(
                    bucket, objectKey, validated.bytes().length, validated.contentType());
        } catch (RestClientException ex) {
            // On a timeout, upload outcome is unknown. Reconcile orphan keys during integration.
            log.warn("Report upload failed or outcome unknown; reconcile object key {}", objectKey);
            throw new BusinessException("Không thể xác nhận lưu file. Vui lòng thử lại sau.");
        }
    }

    /** Call only AFTER checking the requesting user's access to the report in the database. */
    public URI createSignedDownloadUrl(String objectKey) {
        configured();
        checkKey(objectKey);
        try {
            Map<?, ?> body =
                    client.post()
                            .uri(
                                    "/object/sign/{bucket}/{folder}/{file}",
                                    bucket,
                                    "reports",
                                    objectKey.substring(8))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("expiresIn", 300))
                            .retrieve()
                            .body(Map.class);
            if (body == null
                    || !(body.get("signedURL") instanceof String path)
                    || !path.startsWith("/object/sign/"))
                throw new BusinessException("Không tạo được liên kết tải file.");
            return URI.create(baseUrl + "/storage/v1" + path);
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new BusinessException("Không tạo được liên kết tải file. Vui lòng thử lại sau.");
        }
    }

    /**
     * Compensation for a failed DB insert. Do not delete objects belonging to a successful
     * submission.
     */
    public void delete(String objectKey) {
        configured();
        checkKey(objectKey);
        try {
            client.method(HttpMethod.DELETE)
                    .uri("/object/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectKey)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException("Chưa dọn được file trên hệ thống lưu trữ.");
        }
    }

    private void checkKey(String key) {
        if (key == null || !key.matches("reports/[0-9a-f-]{36}\\.(pdf|docx|zip)")) {
            throw new BusinessException("Đường dẫn file không hợp lệ.");
        }
    }

    private void configured() {
        if (baseUrl.isBlank() || apiKey.isBlank() || !bucket.matches("[a-zA-Z0-9_-]+")) {
            throw new BusinessException("Chưa cấu hình dịch vụ lưu trữ báo cáo.");
        }
        URI uri = URI.create(baseUrl);
        if (!"https".equals(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new BusinessException("Cấu hình địa chỉ lưu trữ không hợp lệ.");
        }
    }
}
