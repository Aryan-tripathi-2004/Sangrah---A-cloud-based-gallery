package com.example.Media.application.service.interfaces;

import com.example.Media.api.dto.response.HealthResponse;
import com.example.Media.api.dto.response.MediaDeleteResponse;
import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.api.dto.response.StorageUsageLedgerResponse;
import com.example.Media.api.dto.response.StorageUsageResponse;
import com.example.Media.shared.enums.MediaDomain;
import org.springframework.data.domain.Page;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for all media lifecycle operations.
 */
public interface IMediaService {

    MediaResponse uploadMedia(String userId, MediaDomain domain, String entityRefId, MultipartFile file);

    Resource getMediaFile(String mediaId);

    ResponseEntity<?> getMediaResponse(String mediaId, String callerUserId, String role, String acceptHeader);

    MediaResponse getMediaById(String mediaId);

    default MediaResponse getMediaDetails(String mediaId) {
        return getMediaById(mediaId);
    }

    MediaResponse getAuthorizedMediaDetails(String mediaId, String callerUserId, String role);

    boolean shouldStreamBinary(String acceptHeader);

    MediaDeleteResponse deleteMedia(String mediaId);

    MediaDeleteResponse deleteMedia(String mediaId, String userId, String role);

    MediaDeleteResponse deleteMediaBulk(List<String> mediaIds);

    MediaDeleteResponse deleteMediaBulk(List<String> mediaIds, String userId, String role);

    List<MediaResponse> listMediaByUser(String userId);

    List<MediaResponse> listMediaByUserAndDomain(String userId, MediaDomain domain);

    List<MediaResponse> listAuthorizedMedia(String userId, String callerUserId, String role, MediaDomain domain);

    long getStorageUsageBytes(String userId);

    StorageUsageResponse getStorageUsage(String userId);

    StorageUsageResponse getAuthorizedStorageUsage(String userId, String headerUserId, String role);

    List<StorageUsageLedgerResponse> getStorageLedger(String userId, Instant start, Instant end);

    List<StorageUsageLedgerResponse> getAuthorizedStorageLedger(String userId, Instant startDate, Instant endDate, String headerUserId, String role);

    Page<StorageUsageLedgerResponse> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size);

    HealthResponse getHealth();
}
