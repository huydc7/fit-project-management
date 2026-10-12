package com.example.quanlydetai.storage;

import com.example.quanlydetai.exception.BusinessException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

@Service
public class ReportStorageService {
    private final String backend;
    private final Path root;
    private final ReportFileValidator validator;
    private final SupabaseStorageService cloud;

    public record Download(URI redirect, Resource resource, String filename) {}

    public ReportStorageService(
            @Value("${app.storage.backend:local}") String backend,
            @Value("${app.storage.local-path:./uploads}") String directory,
            ReportFileValidator validator,
            SupabaseStorageService cloud,
            Environment environment) {
        if (!Set.of("local", "supabase").contains(backend))
            throw new IllegalArgumentException("STORAGE_BACKEND must be local or supabase");
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
                && backend.equals("local"))
            throw new IllegalArgumentException("Production requires STORAGE_BACKEND=supabase");
        this.backend = backend;
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.validator = validator;
        this.cloud = cloud;
    }

    public String upload(MultipartFile file) {
        if (backend.equals("supabase")) return "supabase:" + cloud.upload(file).objectKey();
        var valid = validator.validate(file);
        String key = "reports/" + UUID.randomUUID() + "." + valid.extension();
        Path path = localPath(key);
        try {
            Files.createDirectories(path.getParent());
            rejectSymlinks(path);
            Files.write(path, valid.bytes(), StandardOpenOption.CREATE_NEW);
            return "local:" + key;
        } catch (IOException ex) {
            throw new BusinessException("Không thể lưu báo cáo trên máy local.");
        }
    }

    public Download download(String reference) {
        String key = key(reference);
        if (reference.startsWith("supabase:"))
            return new Download(cloud.createSignedDownloadUrl(key), null, null);
        Path path = localPath(key);
        try {
            rejectSymlinks(path);
        } catch (IOException ex) {
            throw new BusinessException("Không đọc được báo cáo.");
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new BusinessException("Không tìm thấy file báo cáo trên ổ lưu trữ.");
        return new Download(null, new FileSystemResource(path), path.getFileName().toString());
    }

    public void delete(String reference) {
        String key = key(reference);
        if (reference.startsWith("supabase:")) {
            cloud.delete(key);
            return;
        }
        try {
            Path path = localPath(key);
            rejectSymlinks(path);
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new BusinessException("Không dọn được file local.");
        }
    }

    private String key(String reference) {
        if (reference == null
                || !(reference.startsWith("local:") || reference.startsWith("supabase:")))
            throw new BusinessException(
                    "Báo cáo dùng đường dẫn lưu trữ cũ; cần chuyển sang object key.");
        String key = reference.substring(reference.indexOf(':') + 1);
        localPath(key);
        return key;
    }

    private Path localPath(String key) {
        if (key == null || !key.matches("reports/[0-9a-f-]{36}\\.(pdf|docx|zip)"))
            throw new BusinessException("Đường dẫn báo cáo không hợp lệ.");
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new BusinessException("Đường dẫn báo cáo không hợp lệ.");
        return path;
    }

    private void rejectSymlinks(Path path) throws IOException {
        for (Path p = path; p != null; p = p.getParent())
            if (Files.isSymbolicLink(p)) throw new IOException("Symbolic links are not allowed");
    }
}
