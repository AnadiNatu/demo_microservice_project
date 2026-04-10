package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.notifcation.EmailService;
import com.microservice_demo.auth_service.notifcation.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin("*")
@RequiredArgsConstructor
public class OtpController {

    private final EmailService emailService;
    private final SmsService smsService;

    @PostMapping("/send/email")
    public ResponseEntity<Map<String, String>> sendEmailOtp(@RequestParam String email) {
        emailService.sendOtpViaEmail(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
    }

    @PostMapping("/verify/email")
    public ResponseEntity<Map<String, Object>> verifyEmailOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        boolean valid = emailService.validateOtp(email, otp);
        if (valid) {
            return ResponseEntity.ok(Map.of("verified", true, "message", "Email verified successfully"));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("verified", false, "message", "Invalid or expired OTP"));
    }

    @PostMapping("/send/sms")
    public ResponseEntity<Map<String, String>> sendSmsOtp(@RequestParam String phone) {
        smsService.sendOtpViaSms(phone);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + phone));
    }

    @PostMapping("/verify/sms")
    public ResponseEntity<Map<String, Object>> verifySmsOtp(
            @RequestParam String phone,
            @RequestParam String otp) {

        boolean valid = smsService.validateOtp(phone, otp);
        if (valid) {
            return ResponseEntity.ok(Map.of("verified", true, "message", "Phone verified successfully"));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("verified", false, "message", "Invalid or expired OTP"));
    }
}
