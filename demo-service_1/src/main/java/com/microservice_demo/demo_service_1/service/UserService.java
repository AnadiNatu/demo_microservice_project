package com.microservice_demo.demo_service_1.service;

import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.repository.UserRepository;
import com.microservice_demo.demo_service_1.service.interfaces.UserServiceInterface;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceInterface {

    private final UserRepository repo;

    private final String uploadFolder = "C:/user-profile-photos/";

    @Override
    public Users createUser(CreateUserDto dto) {

        Users user = new Users();

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        // ✅ FIX: Convert single role string → Set<String>
        Set<String> roles = new HashSet<>();
        roles.add(dto.getUserRole().startsWith("ROLE_")
                ? dto.getUserRole()
                : "ROLE_" + dto.getUserRole().toUpperCase());

        user.setRole(roles);

        user.setDe1ConnectionFlag(false);
        user.setDe2ConnectionFlag(false);

        return repo.save(user);
    }

    @Override
    public Users getUser(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public String uploadPhotoToFolder(Long userId, MultipartFile file) {
        Users user = getUser(userId);

        try {
            Files.createDirectories(Paths.get(uploadFolder));
            String filePath = uploadFolder + userId + "_" + file.getOriginalFilename();

            Files.write(Paths.get(filePath), file.getBytes());
            return "Uploaded to: " + filePath;
        } catch (Exception ex) {
            throw new RuntimeException("Error uploading photo: " + ex.getMessage());
        }
    }

    @Override
    public byte[] getProfilePhotoFromFolder(Long userId) {
        try {
            File folder = new File(uploadFolder);
            File[] files = folder.listFiles((dir, name) -> name.startsWith(userId + "_"));

            if (files == null || files.length == 0)
                throw new ResourceNotFoundException("Photo not found for userId: " + userId);

            return Files.readAllBytes(files[0].toPath());
        } catch (Exception ex) {
            throw new RuntimeException("Error reading photo: " + ex.getMessage());
        }
    }

    @Override
    public String uploadPhotoToCloudinary(Long userId, MultipartFile file) {
        return "Demo URL for userId " + userId;
    }

    @Override
    public String getProfilePhotoFromCloudinary(Long userId) {
        return "Demo URL fetched for userId " + userId;
    }
}
