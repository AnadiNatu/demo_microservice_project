package com.microservice_demo.demo_service_1.controller;
import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.dto.UserSyncDto;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Users create(@RequestBody CreateUserDto dto) {
        return service.createUser(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Users get(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping("/sync")
    public String syncUser(@RequestBody UserSyncDto syncDto) {
        System.out.println("📥 Received user sync request for: " + syncDto.getEmail());

        CreateUserDto dto = new CreateUserDto();
        dto.setName(syncDto.getUsername());
        dto.setEmail(syncDto.getEmail());
        dto.setPhone(""); // No phone in sync data

        // Convert Set<String> roles to single role string
        // Extract the first role, removing "ROLE_" prefix if present
        String role = "USER"; // default
        if (syncDto.getRoles() != null && !syncDto.getRoles().isEmpty()) {
            role = syncDto.getRoles().iterator().next().replace("ROLE_", "");
        }
        dto.setUserRole(role);

        Users createdUser = service.createUser(dto);
        System.out.println("✅ User synced successfully: " + createdUser.getEmail());

        return "User synced successfully to Demo-Service1";
    }

    @PostMapping("/{id}/uploadLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public String uploadLocal(@PathVariable Long id, @RequestParam MultipartFile file) {
        return service.uploadPhotoToFolder(id, file);
    }

    @GetMapping("/{id}/photoLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public byte[] getLocalPhoto(@PathVariable Long id) {
        return service.getProfilePhotoFromFolder(id);
    }
}
