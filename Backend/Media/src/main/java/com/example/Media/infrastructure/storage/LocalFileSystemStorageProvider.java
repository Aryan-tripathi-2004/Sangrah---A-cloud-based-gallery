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

    @Value("${sangrah.storage.root}")
    private String storageRoot;

    @PostConstruct
    public void init() {
        Path resolvedPath = Paths.get(storageRoot).toAbsolutePath().normalize();
        log.info("🔧 Media Storage initialized: {} → {}", storageRoot, resolvedPath);
    }

    @Override
    public String save(MultipartFile file) throws IOException {
        try {
            log.debug("📁 Saving file to local filesystem");

            Path storagePath = Paths.get(storageRoot).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);

            // Discard the potentially malicious client filename completely
            // Generate a completely safe, server-controlled unique identifier
            String uniqueKey = UUID.randomUUID().toString();
            
            // Ensure the resolved path remains strictly within the intended storage boundary
            Path filePath = storagePath.resolve(uniqueKey).normalize();
            if (!filePath.startsWith(storagePath)) {
                throw new SecurityException("Path traversal attempt detected.");
            }

            file.transferTo(filePath.toFile());
            log.info("✅ File saved successfully: {} at {}", uniqueKey, filePath);

            return uniqueKey;

        } catch (IOException e) {
            log.error("❌ Failed to save file: {}", e.getMessage(), e);
            throw new IOException("Failed to save file securely.", e);
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        try {
            Path storagePath = Paths.get(storageRoot).toAbsolutePath().normalize();
            Path filePath = storagePath.resolve(storageKey).normalize();
            
            if (!filePath.startsWith(storagePath)) {
                throw new SecurityException("Path traversal attempt detected.");
            }

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
            Path storagePath = Paths.get(storageRoot).toAbsolutePath().normalize();
            Path filePath = storagePath.resolve(storageKey).normalize();
            
            if (!filePath.startsWith(storagePath)) {
                throw new SecurityException("Path traversal attempt detected.");
            }

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
            Path storagePath = Paths.get(storageRoot).toAbsolutePath().normalize();
            Path filePath = storagePath.resolve(storageKey).normalize();
            
            if (!filePath.startsWith(storagePath)) {
                log.error("❌ Path traversal attempt detected during existence check.");
                return false;
            }
            
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("❌ Error checking file existence: {}", e.getMessage());
            return false;
        }
    }
}