package com.example.quanlydetai.storage;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.util.zip.*;

class ReportFileValidatorTest {
    private final ReportFileValidator validator = new ReportFileValidator();

    @Test
    void acceptsPdfAndUsesDetectedMimeInsteadOfBrowserMime() {
        var result = validator.validate(file("report.PDF", "%PDF-1.7\n%%EOF".getBytes()));
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsExecutableWithPdfExtension() {
        assertThatThrownBy(() -> validator.validate(file("report.pdf", "MZ executable".getBytes())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsEmptyDisallowedAndLimitSize() {
        assertThatThrownBy(() -> validator.validate(file("report.pdf", new byte[0])))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(file("report.exe", "abc".getBytes())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        file(
                                                "report.pdf",
                                                new byte[ReportFileValidator.MAX_BYTES])))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsDocxButRejectsZipRenamedToDocx() throws Exception {
        validator.validate(file("report.docx", zip("[Content_Types].xml", "word/document.xml")));
        assertThatThrownBy(() -> validator.validate(file("report.docx", zip("readme.txt"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsTraversalAndMacros() {
        assertThatThrownBy(() -> validator.validate(file("report.zip", zip("../outside.txt"))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        file(
                                                "report.docx",
                                                zip(
                                                        "[Content_Types].xml",
                                                        "word/document.xml",
                                                        "word/vbaProject.bin"))))
                .isInstanceOf(BusinessException.class);
    }

    private MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/x-untrusted", bytes);
    }

    private byte[] zip(String... names) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("content".getBytes());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
