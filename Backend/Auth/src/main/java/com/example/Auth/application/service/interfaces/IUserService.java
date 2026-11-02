package com.example.Auth.application.service.interfaces;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.UpdateProfileRequest;
import com.example.Auth.api.dto.response.UserWrapperResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    UserDTO getUserById(String userId);
    UserDTO getUserByEmail(String email);
    UserWrapperResponse getUserWrapperById(String userId);
    UserWrapperResponse getUserWrapperByEmail(String email);
    UserWrapperResponse findUserWrapper(String id, String email);
    UserDTO updateUser(String userId, UpdateProfileRequest updateRequest);
    UserDTO uploadAvatar(String userId, MultipartFile file);
    void deleteUser(String userId);
}
