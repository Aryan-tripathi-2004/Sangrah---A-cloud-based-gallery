package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface IGalleryMediaService {
    MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException;
    StorageUsageResponse getStorageUsage(String userId);
    MediaItemResponse getMedia(String userId, String mediaId);
    Resource getMediaFile(String userId, String mediaId);
    void deleteMedia(String userId, String mediaId);
    List<MediaItemResponse> listUserMedia(String userId);
    List<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate);
}
