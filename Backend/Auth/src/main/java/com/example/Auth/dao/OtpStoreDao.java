package com.example.Auth.dao;

import com.example.Auth.entity.OtpStore;
import com.example.Auth.model.OtpStoreModel;
import com.example.Auth.repository.OtpStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for OtpStore operations.
 */
@Component
public class OtpStoreDao extends BaseDao<OtpStore, OtpStoreModel, UUID> {

    @Autowired
    private OtpStoreRepository otpStoreRepository;

    @Override
    protected Class<OtpStore> getEntityClass() {
        return OtpStore.class;
    }

    @Override
    protected OtpStoreModel toModel(OtpStore entity) {
        if (entity == null)
            return null;

        OtpStoreModel model = new OtpStoreModel();
        model.setId(entity.getId());
        model.setOtpHash(entity.getOtpHash());
        model.setEmail(entity.getEmail());
        model.setUserId(entity.getUserId());
        model.setPurpose(
                entity.getPurpose() != null ? OtpStoreModel.Purpose.valueOf(entity.getPurpose().name()) : null);
        model.setExpiresAt(entity.getExpiresAt());
        model.setUsed(entity.isUsed());
        model.setUsedAt(entity.getUsedAt());
        model.setAttempts(entity.getAttempts());
        model.setCreatedAt(entity.getCreatedAt());

        return model;
    }

    @Override
    protected OtpStore toEntity(OtpStoreModel model) {
        if (model == null)
            return null;

        OtpStore entity = new OtpStore();
        entity.setId(model.getId());
        entity.setOtpHash(model.getOtpHash());
        entity.setEmail(model.getEmail());
        entity.setUserId(model.getUserId());
        entity.setPurpose(model.getPurpose() != null ? OtpStore.Purpose.valueOf(model.getPurpose().name()) : null);
        entity.setExpiresAt(model.getExpiresAt());
        entity.setUsed(model.isUsed());
        entity.setUsedAt(model.getUsedAt());
        entity.setAttempts(model.getAttempts());

        return entity;
    }

    // Query methods

    public Optional<OtpStoreModel> findValidOtpByEmailAndPurpose(String email,
            OtpStore.Purpose purpose, Instant now) {
        return otpStoreRepository.findValidOtpByEmailAndPurpose(email, purpose, now)
                .map(this::toModel);
    }

    public List<OtpStoreModel> findByUserId(UUID userId) {
        return otpStoreRepository.findByUserId(userId).stream()
                .map(this::toModel).toList();
    }

    public List<OtpStoreModel> findByEmail(String email) {
        return otpStoreRepository.findByEmail(email).stream()
                .map(this::toModel).toList();
    }

    public long countRecentOtpsByEmailAndPurpose(String email, OtpStore.Purpose purpose, Instant since) {
        return otpStoreRepository.countRecentOtpsByEmailAndPurpose(email, purpose, since);
    }

    public long countActiveOtpsByUserId(UUID userId, Instant now) {
        return otpStoreRepository.countActiveOtpsByUserId(userId, now);
    }

    // Update operations

    @Transactional
    public boolean markAsUsed(UUID otpId) {
        Optional<OtpStore> otpOpt = otpStoreRepository.findById(otpId);
        if (otpOpt.isPresent()) {
            OtpStore otp = otpOpt.get();
            otp.setUsed(true);
            otp.setUsedAt(Instant.now());
            otpStoreRepository.save(otp);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean incrementAttempts(UUID otpId) {
        Optional<OtpStore> otpOpt = otpStoreRepository.findById(otpId);
        if (otpOpt.isPresent()) {
            OtpStore otp = otpOpt.get();
            otp.setAttempts(otp.getAttempts() + 1);
            otpStoreRepository.save(otp);
            return true;
        }
        return false;
    }

    public void invalidateOldOtps(String email, OtpStore.Purpose purpose) {
        otpStoreRepository.deleteByEmailAndPurpose(email, purpose);
    }
}
