package com.example.quanlydetai.storage;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.example.quanlydetai.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseStorageServiceTest {
    private MockRestServiceServer server;
    private SupabaseStorageService storage;
    private final String key = "reports/12345678-1234-1234-1234-123456789abc.pdf";

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        storage =
                new SupabaseStorageService(
                        "https://example.supabase.co",
                        "test-secret",
                        "reports",
                        new ReportFileValidator(),
                        builder);
    }

    @Test
    void uploadGeneratesKeyAndNeverOverwrites() {
        server.expect(
                        request ->
                                assertThat(request.getURI().getPath())
                                        .matches(
                                                "/storage/v1/object/reports/reports/[0-9a-f-]{36}\\.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-upsert", "false"))
                .andExpect(header("Authorization", "Bearer test-secret"))
                .andExpect(content().contentType("application/pdf"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var result =
                storage.upload(
                        new MockMultipartFile(
                                "file", "../../original.pdf", "text/plain", "%PDF-1.7".getBytes()));
        assertThat(result.objectKey()).startsWith("reports/").doesNotContain("original", "..");
        server.verify();
    }

    @Test
    void signsForFiveMinutesAndDeletesExactObject() {
        server.expect(
                        requestTo(
                                "https://example.supabase.co/storage/v1/object/sign/reports/"
                                        + key))
                .andExpect(content().json("{\"expiresIn\":300}"))
                .andRespond(
                        withSuccess(
                                "{\"signedURL\":\"/object/sign/reports/" + key + "?token=test\"}",
                                MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://example.supabase.co/storage/v1/object/reports"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"prefixes\":[\"" + key + "\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(storage.createSignedDownloadUrl(key).toString())
                .startsWith("https://example.supabase.co/storage/v1/object/sign/");
        storage.delete(key);
        server.verify();
    }

    @Test
    void rejectsForgedPathWithoutNetworkCall() {
        assertThatThrownBy(() -> storage.createSignedDownloadUrl("../../other/report.pdf"))
                .isInstanceOf(BusinessException.class);
        server.verify();
    }

    @Test
    void storageFailureDoesNotExposeServerResponseOrKey() {
        server.expect(
                        requestTo(
                                "https://example.supabase.co/storage/v1/object/sign/reports/"
                                        + key))
                .andRespond(withServerError().body("internal token test-secret"));
        assertThatThrownBy(() -> storage.createSignedDownloadUrl(key))
                .isInstanceOf(BusinessException.class)
                .hasMessageNotContaining("test-secret")
                .hasMessageNotContaining("internal");
        server.verify();
    }
}
