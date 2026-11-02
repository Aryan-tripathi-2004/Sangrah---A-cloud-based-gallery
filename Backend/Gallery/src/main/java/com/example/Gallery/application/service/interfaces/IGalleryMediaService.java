package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.GalleryMediaListResponse;
import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.QuotaResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public interface IGalleryMediaService {
    MediaItemResponse uploadMedia(String userId, MultipartFile file);

    default MediaItemResponse upload(MultipartFile file, String userId) {
        return uploadMedia(userId, file);
    }

    StorageUsageResponse getStorageUsage(String userId);
    MediaItemResponse getMedia(String userId, String mediaId);
    Resource getMediaFile(String userId, String mediaId);
    ResponseEntity<?> getMediaResponse(String userId, String mediaId, String acceptHeader);
    MediaDeleteResponse deleteMedia(String userId, String mediaId);
    GalleryMediaListResponse listUserMedia(String userId);
    List<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate);
    Page<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size);
    QuotaResponse getQuota();
}
