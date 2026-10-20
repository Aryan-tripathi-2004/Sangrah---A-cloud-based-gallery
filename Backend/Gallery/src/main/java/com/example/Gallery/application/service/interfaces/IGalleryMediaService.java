package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.GalleryMediaListResponse;
import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.QuotaResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface IGalleryMediaService {
    MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException;
    StorageUsageResponse getStorageUsage(String userId);
    MediaItemResponse getMedia(String userId, String mediaId);
    Resource getMediaFile(String userId, String mediaId);
    MediaDeleteResponse deleteMedia(String userId, String mediaId);
    GalleryMediaListResponse listUserMedia(String userId);
    List<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate);
    Page<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size);
    QuotaResponse getQuota();
}
