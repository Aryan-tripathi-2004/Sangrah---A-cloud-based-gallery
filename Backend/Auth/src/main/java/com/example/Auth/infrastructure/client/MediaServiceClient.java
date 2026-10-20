package com.example.Auth.infrastructure.client;

import com.example.Auth.infrastructure.client.dto.MediaUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service-auth", url = "${media.service.url:http://localhost:8087}")
public interface MediaServiceClient {

    @PostMapping(value = "/api/v1/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaUploadResponse uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("domain") String domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId,
            @RequestHeader("X-User-Id") String userId
    );
}
