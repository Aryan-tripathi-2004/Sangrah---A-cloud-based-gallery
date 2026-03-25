package com.example.Gallery.api.dto.response;

import lombok.*;

import java.util.List;

/**
 * Response showing current storage usage (informational, NOT a quota limit)
 * Pay-as-you-use model: users can upload unlimited, only charged for what they use
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageResponse {
    private long totalBytesUsed;  // Total size of all non-deleted files
    private long fileCount;        // Number of non-deleted files
    private long imageBytes;       // Bytes used by images
    private long videoBytes;       // Bytes used by videos
    private String oldestFile;     // Date of oldest file
    private String newestFile;     // Date of newest file
    private String formattedUsage; // Human-readable format (e.g., "1.5 GB")

    public void calculateUsage(List<MediaItemResponse> files) {
        this.fileCount = files.size();
        this.totalBytesUsed = 0;
        this.imageBytes = 0;
        this.videoBytes = 0;

        for (MediaItemResponse file : files) {
            this.totalBytesUsed += file.getSizeBytes();

            if ("IMAGE".equals(file.getType())) {
                this.imageBytes += file.getSizeBytes();
            } else if ("VIDEO".equals(file.getType())) {
                this.videoBytes += file.getSizeBytes();
            }
        }

        this.formattedUsage = formatBytes(this.totalBytesUsed);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
