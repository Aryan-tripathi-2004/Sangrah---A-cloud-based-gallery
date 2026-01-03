package com.example.Auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Business model for ProfilePicture.
 * Used in the service layer for profile picture management.
 * Decoupled from database entity annotations.
 */
public class ProfilePictureModel {
    private UUID id;
    private UUID userId;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Integer width;
    private Integer height;
    private boolean current;
    private Instant createdAt;

    // Constructors

    public ProfilePictureModel() {
    }

    public ProfilePictureModel(UUID userId, String storageKey, String originalFilename) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.current = false;
        this.createdAt = Instant.now();
    }

    // Business Logic Methods

    /**
     * Check if this is an image type.
     *
     * @return true if image
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Mark this picture as current.
     */
    public void markAsCurrent() {
        this.current = true;
    }

    /**
     * Unmark this picture as current.
     */
    public void unmarkAsCurrent() {
        this.current = false;
    }

    /**
     * Get file size in kilobytes.
     *
     * @return size in KB
     */
    public double getSizeInKB() {
        return sizeBytes != null ? sizeBytes / 1024.0 : 0;
    }

    /**
     * Get file size in megabytes.
     *
     * @return size in MB
     */
    public double getSizeInMB() {
        return sizeBytes != null ? sizeBytes / (1024.0 * 1024.0) : 0;
    }

    /**
     * Check if the picture is large (over threshold).
     *
     * @param thresholdMB threshold in megabytes
     * @return true if larger than threshold
     */
    public boolean isLarge(double thresholdMB) {
        return getSizeInMB() > thresholdMB;
    }

    /**
     * Get aspect ratio.
     *
     * @return aspect ratio (width/height), or 0 if dimensions not set
     */
    public double getAspectRatio() {
        if (width != null && height != null && height > 0) {
            return (double) width / height;
        }
        return 0;
    }

    /**
     * Check if this is a square image.
     *
     * @param tolerance tolerance for aspect ratio (e.g., 0.1 for 10%)
     * @return true if approximately square
     */
    public boolean isSquare(double tolerance) {
        double aspectRatio = getAspectRatio();
        return aspectRatio > 0 && Math.abs(aspectRatio - 1.0) <= tolerance;
    }

    /**
     * Get file extension from filename.
     *
     * @return file extension or empty string
     */
    public String getFileExtension() {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return "";
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
