package com.example.Media.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileSystemStorageProvider implements StorageProvider {

    @Value("${sangrah.storage.root:./storage}")
    private String storageRoot;

    @PostConstruct
    public void init() {
        Path resolvedPath = Paths.get(storageRoot).toAbsolutePath();
        log.info("🔧 Media Storage initialized: {} → {}", storageRoot, resolvedPath);
    }

    @Override
    public String save(MultipartFile file) throws IOException {
        try {
            log.debug("📁 Saving file to local filesystem: {}", file.getOriginalFilename());

            // Resolve storage path - handle both relative and absolute paths
            Path storagePath = Paths.get(storageRoot).toAbsolutePath();
            log.debug("📂 Storage path resolved to: {}", storagePath);

            // Create storage directory if not exists
            Files.createDirectories(storagePath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String uniqueKey = UUID.randomUUID() + "-" + originalFilename;
            Path filePath = storagePath.resolve(uniqueKey);

            // Save file
            file.transferTo(filePath.toFile());
            log.info("✅ File saved successfully: {} at {}", uniqueKey, filePath);

            return uniqueKey;

        } catch (IOException e) {
            log.error("❌ Failed to save file: {}", e.getMessage(), e);
            throw new IOException("Failed to save file: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        try {
            Path storagePath = Paths.get(storageRoot).toAbsolutePath();
            Path filePath = storagePath.resolve(storageKey);

            if (!Files.exists(filePath)) {
                log.warn("⚠️ File not found: {} (looked in {})", storageKey, filePath);
                throw new IOException("File not found: " + storageKey);
            }

            log.info("📁 Loading file from local filesystem: {} from {}", storageKey, filePath);
            return new FileSystemResource(filePath);

        } catch (IOException e) {
            log.error("❌ Failed to load file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            Path storagePath = Paths.get(storageRoot).toAbsolutePath();
            Path filePath = storagePath.resolve(storageKey);

            if (!Files.exists(filePath)) {
                log.warn("⚠️ File not found for deletion: {}", storageKey);
                return;
            }

            Files.delete(filePath);
            log.info("✅ File deleted successfully: {}", storageKey);

        } catch (IOException e) {
            log.error("❌ Failed to delete file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path storagePath = Paths.get(storageRoot).toAbsolutePath();
            Path filePath = storagePath.resolve(storageKey);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("❌ Error checking file existence: {}", e.getMessage());
            return false;
        }
    }
}
