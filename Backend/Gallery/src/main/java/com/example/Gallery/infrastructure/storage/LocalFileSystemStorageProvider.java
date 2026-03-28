package com.example.Gallery.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalFileSystemStorageProvider implements StorageProvider {
    private final Path rootPath;

    public LocalFileSystemStorageProvider(@Value("${sangrah.storage.root}") String root) throws IOException {
        this.rootPath = Paths.get(root).toAbsolutePath();
        Files.createDirectories(this.rootPath);
    }

    @Override
    public String save(MultipartFile file) throws IOException {
        String key = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path target = rootPath.resolve(key);
        file.transferTo(target);
        return key;
    }

    @Override
    public Resource load(String storageKey) {
        return new FileSystemResource(rootPath.resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(rootPath.resolve(storageKey));
        } catch (IOException ignored) {
        }
    }
}
