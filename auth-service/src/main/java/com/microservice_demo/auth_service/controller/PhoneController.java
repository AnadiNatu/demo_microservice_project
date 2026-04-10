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

        String normalizedNum = normalizePhone(phone);
        log.info("[PHONE_AUTH] OTP sent | phone={}", phone);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to " + phone,
                "phone", phone,
                "note", "OTP expires in 5 minutes"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyPhoneOtp(@RequestParam String phone, @RequestParam String otp) {
        log.info("[PHONE_AUTH] OTP verification | phone={}", phone);

        String normalizedNum = normalizePhone(phone);
        boolean valid = smsService.validateOtp(normalizedNum, otp);
        if (!valid) {
            throw new ("phoneLogin", "Invalid or expired OTP");
        }

        var type1User = userRepository.findAll().stream().filter(u -> normalizedNum.equals(normalizePhone(u.getPhoneNumber()))).findFirst();

        if (type1User.isPresent()) {
            var entity = type1User.get();
            Users domain = new Users();
            domain.setId(entity.getId());
            domain.setEmail(entity.getEmail());
//            domain.setFname(entity.getFname());
//            domain.setLname(entity.getLname());
            domain.setPassword(entity.getPassword());
            domain.setPhoneNumber(entity.getPhoneNumber());
            domain.setRoles();

            String token = jwtUtil.generateTokenFromUser();
            log.info("[PHONE_AUTH] TYPE1 login via phone | id={} | phone={}", entity.getId(), phone);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userType", "TYPE1",
                    "role", entity.getRole(),
                    "email", entity.getEmail(),
                    "message", "Phone login successful"
            ));
        }
        throw new RuntimeException("User not found");
    }

    private String normalizePhone(String phone){
        if (phone == null) return null;

        phone = phone.trim().replace("[^\\d]" , "");

        if (phone.length() > 10){
            return phone.substring(phone.length() - 10);
        }
        return phone;
    }
}

