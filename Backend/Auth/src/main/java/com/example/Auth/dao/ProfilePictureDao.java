package com.example.Auth.dao;

import com.example.Auth.entity.ProfilePicture;
import com.example.Auth.model.ProfilePictureModel;
import com.example.Auth.repository.ProfilePictureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for ProfilePicture operations.
 */
@Component
public class ProfilePictureDao extends BaseDao<ProfilePicture, ProfilePictureModel, UUID> {

    @Autowired
    private ProfilePictureRepository profilePictureRepository;

    @Override
    protected Class<ProfilePicture> getEntityClass() {
        return ProfilePicture.class;
    }

    @Override
    protected ProfilePictureModel toModel(ProfilePicture entity) {
        if (entity == null)
            return null;

        ProfilePictureModel model = new ProfilePictureModel();
        model.setId(entity.getId());
        model.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        model.setStorageKey(entity.getStorageKey());
        model.setOriginalFilename(entity.getFilename());
        model.setContentType(entity.getContentType());
        model.setSizeBytes(entity.getSizeBytes());
        model.setWidth(entity.getWidth());
        model.setHeight(entity.getHeight());
        model.setCurrent(entity.isCurrent());
        model.setCreatedAt(entity.getCreatedAt());

        return model;
    }

    @Override
    protected ProfilePicture toEntity(ProfilePictureModel model) {
        if (model == null)
            return null;

        ProfilePicture entity = new ProfilePicture();
        entity.setId(model.getId());
        entity.setStorageKey(model.getStorageKey());
        entity.setFilename(model.getOriginalFilename());
        entity.setContentType(model.getContentType());
        entity.setSizeBytes(model.getSizeBytes());
        entity.setWidth(model.getWidth());
        entity.setHeight(model.getHeight());
        entity.setCurrent(model.isCurrent());

        return entity;
    }

    // Query methods

    public List<ProfilePictureModel> findByUserId(UUID userId) {
        return profilePictureRepository.findByUserId(userId).stream()
                .map(this::toModel).toList();
    }

    public Optional<ProfilePictureModel> findCurrentByUserId(UUID userId) {
        return profilePictureRepository.findCurrentByUserId(userId).map(this::toModel);
    }

    public Optional<ProfilePictureModel> findByStorageKey(String storageKey) {
        return profilePictureRepository.findByStorageKey(storageKey).map(this::toModel);
    }

    public boolean existsCurrentByUserId(UUID userId) {
        return profilePictureRepository.existsCurrentByUserId(userId);
    }

    public long countByUserId(UUID userId) {
        return profilePictureRepository.countByUserId(userId);
    }

    public long calculateTotalStorageByUserId(UUID userId) {
        List<ProfilePicture> pictures = profilePictureRepository.findAllByUserId(userId);
        return pictures.stream()
                .mapToLong(p -> p.getSizeBytes() != null ? p.getSizeBytes() : 0)
                .sum();
    }

    // Update operations

    @Transactional
    public boolean setCurrentPicture(UUID userId, UUID pictureId) {
        // First, unmark all as current
        List<ProfilePicture> pictures = profilePictureRepository.findByUserId(userId);
        for (ProfilePicture picture : pictures) {
            if (picture.isCurrent()) {
                picture.setCurrent(false);
                profilePictureRepository.save(picture);
            }
        }

        // Then, mark the specified picture as current
        Optional<ProfilePicture> pictureOpt = profilePictureRepository.findById(pictureId);
        if (pictureOpt.isPresent()) {
            ProfilePicture picture = pictureOpt.get();
            picture.setCurrent(true);
            profilePictureRepository.save(picture);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean unmarkAllAsCurrentForUser(UUID userId) {
        List<ProfilePicture> pictures = profilePictureRepository.findByUserId(userId);
        boolean anyUpdated = false;
        for (ProfilePicture picture : pictures) {
            if (picture.isCurrent()) {
                picture.setCurrent(false);
                profilePictureRepository.save(picture);
                anyUpdated = true;
            }
        }
        return anyUpdated;
    }
}
