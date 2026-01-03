package com.example.Auth.dao;

import com.example.Auth.entity.AuditLog;
import com.example.Auth.model.AuditLogModel;
import com.example.Auth.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DAO for AuditLog operations.
 */
@Component
public class AuditLogDao extends BaseDao<AuditLog, AuditLogModel, UUID> {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    protected Class<AuditLog> getEntityClass() {
        return AuditLog.class;
    }

    @Override
    protected AuditLogModel toModel(AuditLog entity) {
        if (entity == null)
            return null;

        AuditLogModel model = new AuditLogModel();
        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setAction(entity.getAction());
        model.setEntityType(entity.getEntityType());
        model.setEntityId(entity.getEntityId());
        model.setIpAddress(entity.getIpAddress());
        model.setUserAgent(entity.getUserAgent());
        model.setDetails(entity.getDetails());
        model.setCreatedAt(entity.getCreatedAt());

        return model;
    }

    @Override
    protected AuditLog toEntity(AuditLogModel model) {
        if (model == null)
            return null;

        AuditLog entity = new AuditLog();
        entity.setId(model.getId());
        entity.setUserId(model.getUserId());
        entity.setAction(model.getAction());
        entity.setEntityType(model.getEntityType());
        entity.setEntityId(model.getEntityId());
        entity.setIpAddress(model.getIpAddress());
        entity.setUserAgent(model.getUserAgent());
        entity.setDetails(model.getDetails());

        return entity;
    }

    // Query methods

    public Page<AuditLogModel> findByUserId(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(this::toModel);
    }

    public Page<AuditLogModel> findByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::toModel);
    }

    public Page<AuditLogModel> findByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable).map(this::toModel);
    }

    public Page<AuditLogModel> findByUserIdAndDateRange(UUID userId, Instant startDate,
            Instant endDate, Pageable pageable) {
        return auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate, pageable)
                .map(this::toModel);
    }

    public Page<AuditLogModel> findFailedLoginsSince(Instant since, Pageable pageable) {
        return auditLogRepository.findFailedLoginsSince(since, pageable).map(this::toModel);
    }

    public long countFailedLoginsByIpSince(String ipAddress, Instant since) {
        return auditLogRepository.countFailedLoginsByIpSince(ipAddress, since);
    }

    public long countFailedLoginsByEmailSince(String email, Instant since) {
        return auditLogRepository.countFailedLoginsByEmailSince(email, since);
    }

    public long countByAction(String action) {
        return auditLogRepository.countByAction(action);
    }

    public long countByUserId(UUID userId) {
        return auditLogRepository.countByUserId(userId);
    }
}
