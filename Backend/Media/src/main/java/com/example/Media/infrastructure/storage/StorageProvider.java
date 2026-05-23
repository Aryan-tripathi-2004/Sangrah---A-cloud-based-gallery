package com.example.Media.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface StorageProvider {

    /**
     * Save a file and return the storage key (path/reference)
     */
    String save(MultipartFile file) throws IOException;

    /**
     * Load a file by storage key as a Spring Resource
     */
    Resource load(String storageKey) throws IOException;

    /**
     * Delete a file by storage key
     */
    void delete(String storageKey) throws IOException;

    /**
     * Check if file exists
     */
    boolean exists(String storageKey);
}
