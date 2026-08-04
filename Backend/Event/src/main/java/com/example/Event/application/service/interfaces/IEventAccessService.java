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
import jakarta.servlet.http.HttpServletRequest;

public interface IEventAccessService {
    AccessRequestMutationResponse requestAccess(String eventId, AccessMessageRequest request, HttpServletRequest httpRequest);

    AccessRequestsResponse listAccessRequests(String eventId, HttpServletRequest httpRequest);

    AccessApprovalResponse approveRequest(String eventId, String requestId, AccessApprovalRequest request, HttpServletRequest httpRequest);

    AccessRejectionResponse rejectRequest(String eventId, String requestId, AccessRejectionRequest request, HttpServletRequest httpRequest);

    AccessRevocationResponse revokeRequest(String eventId, String requestId, HttpServletRequest httpRequest);

    AccessRequestMutationResponse reRequestAccess(String eventId, AccessMessageRequest request, HttpServletRequest httpRequest);

    AccessStatusResponse getAccessStatus(String eventId, HttpServletRequest httpRequest);

    boolean isUserApproved(String eventId, String requesterUserId);
}
