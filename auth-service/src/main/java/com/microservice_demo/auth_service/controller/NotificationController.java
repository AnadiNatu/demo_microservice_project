package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.notifcation.NotificationService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

//  Sends OTP simultaneously to email AND phone (SMS).
    @PostMapping("otp/dual")
    public ResponseEntity<Map<String , Object>> sendDualChannelOtp(@RequestBody Map<String , String> request){

        String email = request.get("email");
        String phone = request.get("phone");

        if (email == null || phone == null){
            return ResponseEntity.badRequest().body(Map.of(
                    "error" , "Fields 'email' and 'phone' are required"));
        }

        log.info("[NOTIFICATION] Dual-channel OTP request | email={} | phone={}" , email , phone);

        notificationService.sendDualChannelOtp(email , phone);
        return ResponseEntity.ok(Map.of(
                "message" , "OTP sent to email and phone",
                "email" , email,
                "phone" , phone ,
                "expiresIn" , "5 minutes"
        ));
    }

//  Sends a welcome email AND SMS to a newly registered user.
    @PostMapping("welcome")
    public ResponseEntity<Map<String , Object>> sendWelcomeNotification(@RequestBody Map<String , String> request){

        String email = request.get("email");
        String phone = request.get("phone");
        String firstName = request.get("firstName");

        if (email == null || phone == null || firstName == null){
            return ResponseEntity.badRequest().body(Map.of(
                    "error" , "Fields 'email' , 'phone' and 'firstName' are required"));
        }

        log.info("[NOTIFICATION] Welcome notification request | email={}", email);
        notificationService.sendWelcomeNotification(email , phone , firstName);

        return ResponseEntity.ok(Map.of(
                "message" , "Welcome notification sent via email and SMS",
                "email" , email,
                "phone" , phone
        ));
    }

//  Sends a password-reset OTP to the provided email address.
@PostMapping("/password-reset-otp")
public ResponseEntity<Map<String, Object>> sendPasswordResetOtp(
        @RequestBody Map<String, String> request) {

    String email = request.get("email");

    if (email == null) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Field 'email' is required"
        ));
    }

    log.info("[NOTIFICATION] Password reset OTP request | email={}", email);
    notificationService.sendPasswordResetOtp(email);

    return ResponseEntity.ok(Map.of(
            "message",   "Password reset OTP sent to " + email,
            "email",     email,
            "expiresIn", "5 minutes"
    ));
}

//  Sends a login-alert SMS to the user's phone number.
@PostMapping("/login-alert")
public ResponseEntity<Map<String, Object>> sendLoginAlert(
        @RequestBody Map<String, String> request) {

    String phone     = request.get("phone");
    String firstName = request.get("firstName");

    if (phone == null || firstName == null) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Fields 'phone' and 'firstName' are required"
        ));
    }

    log.info("[NOTIFICATION] Login alert request | phone={}", phone);
    notificationService.sendLoginAlert(phone, firstName);

    return ResponseEntity.ok(Map.of(
            "message", "Login alert sent",
            "phone",   phone
    ));
}
}
