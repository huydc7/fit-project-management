package com.example.quanlydetai.storage;

import com.example.quanlydetai.exception.BusinessException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

@Component
public class ReportFileValidator {
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    public record ValidatedFile(byte[] bytes, String extension, String contentType) {}

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() >= MAX_BYTES) {
            throw new BusinessException("Chọn file không rỗng và nhỏ hơn 20 MiB.");
        }
        String name = Objects.toString(file.getOriginalFilename(), "");
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("pdf", "docx", "zip").contains(extension)) {
            throw new BusinessException("Chỉ chấp nhận file PDF, DOCX hoặc ZIP.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES);
            if (bytes.length == 0 || bytes.length >= MAX_BYTES)
                throw new BusinessException("File phải nhỏ hơn 20 MiB.");
            if (extension.equals("pdf")) {
                if (bytes.length < 5
                        || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                    throw new BusinessException("Nội dung file không đúng định dạng PDF.");
                }
                return new ValidatedFile(bytes, extension, "application/pdf");
            }
            validateArchive(bytes, extension.equals("docx"));
            return new ValidatedFile(
                    bytes,
                    extension,
                    extension.equals("zip")
                            ? "application/zip"
                            : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (IOException ex) {
            throw new BusinessException("Không đọc được file hoặc file nén bị hỏng.");
        }
    }

    private void validateArchive(byte[] bytes, boolean docx) throws IOException {
        if (bytes.length < 4
                || bytes[0] != 'P'
                || bytes[1] != 'K'
                || bytes[2] != 3
                || bytes[3] != 4) {
            throw new BusinessException("Nội dung file không đúng định dạng ZIP/DOCX.");
        }
        Set<String> names = new HashSet<>();
        long expanded = 0;
        int count = 0;
        // Inspect within bounded memory/expanded size; never extract anything to disk.
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 2000) throw new BusinessException("File nén có quá nhiều mục.");
                String path = entry.getName().replace('\\', '/');
                if (path.startsWith("/")
                        || path.contains(":")
                        || Arrays.asList(path.split("/")).contains("..")) {
                    throw new BusinessException("File nén chứa đường dẫn không hợp lệ.");
                }
                if (!names.add(path)) throw new BusinessException("File nén chứa mục trùng tên.");
                if (docx && path.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) {
                    throw new BusinessException("Không chấp nhận tài liệu chứa macro.");
                }
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expanded += read;
                    if (expanded > 40L * 1024 * 1024)
                        throw new BusinessException("Nội dung giải nén vượt giới hạn 40 MiB.");
                }
            }
        }
        if (count == 0
                || (docx
                        && !(names.contains("[Content_Types].xml")
                                && names.contains("word/document.xml")))) {
            throw new BusinessException("File ZIP/DOCX không hợp lệ.");
        }
    }
}
