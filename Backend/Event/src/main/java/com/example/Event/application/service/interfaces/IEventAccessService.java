package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.AccessApprovalRequest;
import com.example.Event.api.dto.request.AccessMessageRequest;
import com.example.Event.api.dto.request.AccessRejectionRequest;
import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;

public interface IEventAccessService {
    AccessRequestMutationResponse requestAccess(String eventId, AccessMessageRequest request, String userId);

    AccessRequestsResponse listAccessRequests(String eventId, String userId);

    AccessApprovalResponse approveRequest(String eventId, String requestId, AccessApprovalRequest request, String userId);

    AccessRejectionResponse rejectRequest(String eventId, String requestId, AccessRejectionRequest request, String userId);

    AccessRevocationResponse revokeRequest(String eventId, String requestId, String userId);

    AccessRequestMutationResponse reRequestAccess(String eventId, AccessMessageRequest request, String userId);

    AccessStatusResponse getAccessStatus(String eventId, String userId);

    boolean isUserApproved(String eventId, String requesterUserId);
}
