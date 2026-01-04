package com.example.Auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user's profile picture.
 * Stores metadata about uploaded profile pictures including storage location,
 * dimensions, and size. The actual image is stored in cloud storage (Azure
 * Blob).
 */
@Entity
@Table(name = "profile_pictures", indexes = {
        @Index(name = "idx_profile_picture_user", columnList = "user_id"),
        @Index(name = "idx_profile_picture_storage", columnList = "storage_key", unique = true),
        @Index(name = "idx_profile_picture_current", columnList = "current")
})
@EntityListeners(AuditingEntityListener.class)
public class ProfilePicture {

    /**
     * Maximum allowed file size: 10 MB
     */
    public static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    /**
     * Allowed content types for profile pictures
     */
    public static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    };

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Original filename as uploaded by the user.
     */
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /**
     * MIME type of the image file.
     * Must be one of: image/jpeg, image/png, image/webp, image/gif
     */
    @NotBlank(message = "Content type is required")
    @Pattern(regexp = "^image/(jpeg|png|webp|gif)$", message = "Invalid content type. Allowed: jpeg, png, webp, gif")
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /**
     * File size in bytes.
     * Maximum allowed: 10 MB (10,485,760 bytes)
     */
    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = MAX_SIZE_BYTES, message = "File size cannot exceed 10 MB")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * Storage key/path in cloud storage (Azure Blob Storage).
     * Example: "profile-pictures/user-uuid/image-uuid.jpg"
     */
    @NotBlank(message = "Storage key is required")
    @Size(max = 500, message = "Storage key cannot exceed 500 characters")
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    /**
     * Image width in pixels.
     */
    @Min(value = 1, message = "Width must be at least 1 pixel")
    @Column(name = "width")
    private Integer width;

    /**
     * Image height in pixels.
     */
    @Min(value = 1, message = "Height must be at least 1 pixel")
    @Column(name = "height")
    private Integer height;

    /**
     * Flag indicating if this is the user's current profile picture.
     * Only one picture per user should be marked as current.
     */
    @Column(name = "current", nullable = false)
    private boolean current = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors

    public ProfilePicture() {
        this.id = UUID.randomUUID();
    }

    public ProfilePicture(User user, String filename, String contentType, Long sizeBytes, String storageKey) {
        this();
        this.user = user;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
    }

    // Business Methods

    /**
     * Check if this is an image content type.
     *
     * @param contentType the content type to check
     * @return true if the content type is allowed
     */
    public static boolean isAllowedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        for (String allowed : ALLOWED_CONTENT_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the file size is within the allowed limit.
     *
     * @param sizeBytes the file size in bytes
     * @return true if the size is valid
     */
    public static boolean isValidSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_SIZE_BYTES;
    }

    /**
     * Get the file extension from the filename.
     *
     * @return the file extension (e.g., "jpg", "png") or empty string if none
     */
    public String getFileExtension() {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Get a human-readable file size string.
     *
     * @return formatted file size (e.g., "2.5 MB")
     */
    public String getFormattedSize() {
        if (sizeBytes == null) {
            return "0 B";
        }
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }

    /**
     * Calculate the aspect ratio of the image.
     *
     * @return aspect ratio (width/height) or 0 if dimensions are not set
     */
    public double getAspectRatio() {
        if (width == null || height == null || height == 0) {
            return 0.0;
        }
        return (double) width / height;
    }

    /**
     * Mark this picture as the current profile picture.
     */
    public void markAsCurrent() {
        this.current = true;
    }

    /**
     * Mark this picture as not current.
     */
    public void markAsNotCurrent() {
        this.current = false;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProfilePicture))
            return false;
        ProfilePicture that = (ProfilePicture) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProfilePicture{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", width=" + width +
                ", height=" + height +
                ", current=" + current +
                ", createdAt=" + createdAt +
                '}';
    }
}
