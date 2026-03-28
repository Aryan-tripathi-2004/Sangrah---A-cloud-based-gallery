package com.example.Gallery.infrastructure.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageProvider {
    String save(MultipartFile file) throws IOException;
    Resource load(String storageKey);
    void delete(String storageKey);
}
