package com.microservice_demo.demo_service_1.controller;
import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping
    public Users create(@RequestBody CreateUserDto dto) {
        return service.createUser(dto);
    }

    @GetMapping("/{id}")
    public Users get(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping("/{id}/uploadLocal")
    public String uploadLocal(@PathVariable Long id, @RequestParam MultipartFile file) {
        return service.uploadPhotoToFolder(id, file);
    }

    @GetMapping("/{id}/photoLocal")
    public byte[] getLocalPhoto(@PathVariable Long id) {
        return service.getProfilePhotoFromFolder(id);
    }
}
