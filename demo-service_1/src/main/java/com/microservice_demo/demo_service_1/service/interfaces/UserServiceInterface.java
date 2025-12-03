package com.microservice_demo.demo_service_1.service.interfaces;

import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.entity.Users;
import org.springframework.web.multipart.MultipartFile;

public interface UserServiceInterface {

    Users createUser(CreateUserDto dto);
    Users getUser(Long id);

    String uploadPhotoToFolder(Long userId, MultipartFile file);
    byte[] getProfilePhotoFromFolder(Long userId);

    String uploadPhotoToCloudinary(Long userId, MultipartFile file);
    String getProfilePhotoFromCloudinary(Long userId);
}
