package com.microservice_demo.auth_service.controller;


import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.notifcation.SmsService;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/phone/")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class PhoneController {

    private final SmsService smsService;
    private final JwtTokenProvider jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("send-otp")
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(@RequestParam String phone) {
        log.info("[PHONE_AUTH] OTP requested | phone={}", phone);

        try{
            String normalizeNum = normalizePhone(phone);
            smsService.sendOtpViaSms(normalizeNum);

            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to " + phone,
                    "phone", phone,
                    "note", "OTP expires in 5 minutes"
            ));
        }catch (Exception ex){
            log.error("[PHONE_AUTH] Failed to send OTP | phone={} | error={}", phone, ex.getMessage());

            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to send OTP",
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("verify-otp")
    public ResponseEntity<Map<String, Object>> verifyPhoneOtp(@RequestParam String phone, @RequestParam String otp) {
        log.info("[PHONE_AUTH] OTP verification | phone={}", phone);

        try{
            String normalizedNum = normalizePhone(phone);
            boolean valid = smsService.validateOtp(normalizedNum , otp);

            if (!valid){
                log.warn("[PHONE_AUTH] Invalid OTP | phone={}", phone);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid or expired OTP",
                        "message", "Please request a new OTP"
                ));
            }

            var userOptional = userRepository.findByPhoneNumber(normalizedNum);
            if (userOptional.isEmpty()){
                log.warn("[PHONE_AUTH] User not found | phone={}", phone);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "User not found",
                        "message", "No account exists with this phone number. Please register first."
                ));
            }

            Users user = userOptional.get();

            String token = jwtUtil.generateTokenFromUser(user);
            log.info("[PHONE_AUTH] Login successful | userId={} | phone={}", user.getId(), phone);

            return ResponseEntity.ok(Map.of(
                    "token" , token ,
                    "username" , user.getUsername(),
                    "email"  , user.getEmail(),
                    "roles" , user.getRoles(),
                    "expires" , jwtUtil.getExpirationMs(),
                    "message" , "Phone login successful"
            ));
        }catch (Exception ex){
            log.error("[PHONE_AUTH] Verification failed | phone = {} | error = {}" , phone , ex.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Verification failed",
                    "message", ex.getMessage()
            ));
        }
    }

    private String normalizePhone(String phone){
        if (phone == null) return null;

        phone = phone.trim().replaceAll("[^\\d+]" , "");

        if (phone.startsWith("+")){
            return phone;
        }

        if (phone.startsWith("91") && phone.length() > 10){
            return "+" + phone;
        }

        if (phone.length() == 10) {
            return "+91" + phone;
        }

        return phone;
    }
}

