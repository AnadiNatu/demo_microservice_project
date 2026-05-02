package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.notifcation.EmailService;
import com.microservice_demo.auth_service.notifcation.SmsService;
import com.microservice_demo.auth_service.otp.OtpStore;
import com.microservice_demo.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/password/")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final EmailService emailService;
    private final SmsService smsService;
    private final OtpStore otpStore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("forgot")
    public ResponseEntity<Map<String , Object>> forgotPassword(@RequestParam String email,  @RequestParam(defaultValue = "email") String method) {
        email = email.trim();

        log.info("[PASSWORD_RESET] Request initiated | email={} | method={}", email, method);

        Optional<Users> user = userRepository.findByEmail(email);

        if (user.isEmpty()){
            log.warn("[PASSWORD_RESET] User not found | email={}", email);
            return ResponseEntity.ok(Map.of(
                    "message" , "If an account exists with this email , you will receive an OTP"
            ));
        }

        Users savedUser = user.get();
        if ("sms".equalsIgnoreCase(method)){
            String phoneNumber = savedUser.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.isBlank()){
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No phone registered for this account",
                        "suggestion", "Use email method instead"
                ));
            }

            smsService.sendOtpViaSms(phoneNumber);
            return ResponseEntity.ok(Map.of(
                    "message" , "OTP sent to your phone number" ,
                    "method" , "sms" ,
                    "phone" , maskPhone(phoneNumber),
                    "expiresIn" , "5 minutes"
            ));
        }

//        Default to email
        emailService.sendOtpViaEmail(email);

        return ResponseEntity.ok(Map.of(
                "message" , "OTP sent to you email instead",
                "method" , "email",
                "email" , maskEmail(email),
                "expiresIn" , "5 minutes"
        ));
    }

    @PostMapping("reset")
    public ResponseEntity<Map<String , Object>> resetPassword(@RequestParam(name = "identifier")String identifier ,@RequestParam(name = "otp")String otp , @RequestParam(name = "newPassword")String newPassword) {
        log.info("[PASSWORD_RESET] Reset attempt | identifier={}", identifier);

        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Password must be at least 8 characters long"
            ));
        }

        String normalizeIdentifier = identifier;
        if (identifier.matches("^\\+?\\d+$")) {
            normalizeIdentifier = formatToE164(identifier);
        }

        boolean valid = otpStore.validate(normalizeIdentifier, otp);
        if (!valid) {
            log.warn("[PASSWORD_RESET] Invalid OTP | identifier={}", identifier);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid or expired OTP",
                    "suggestion", "Request a new OTP"
            ));
        }

        Optional<Users> userOptional = userRepository.findByEmail(normalizeIdentifier);
        if (userOptional.isEmpty()){
            userOptional = userRepository.findByPhoneNumber(normalizeIdentifier);
        }

        if (userOptional.isEmpty()){
            log.error("[PASSWORD_RESET] User not found | identifier={}", identifier);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found"
            ));
        }

        Users user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("[PASSWORD_RESET] Password updated successfully | email={}", user.getEmail());
        try{
            emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Password Changed Successfully",
                    "Hi \" + user.getUsername() + \",\\n\\nYour password has been successfully changed.\\n\\n\" +\n" +
                            "                            \"If you didn't make this change, please contact support immediately."
            );
        }catch (Exception e){
            log.warn("[PASSWORD_RESET] Confirmation email failed | email={}", user.getEmail());
        }

        return ResponseEntity.ok(Map.of(
                "message" , "Password reset successful",
                "email" , user.getEmail()
        ));
    }

    @PostMapping("change")
    public ResponseEntity<Map<String,Object>> changePassword(@RequestParam(name = "email")String email, @RequestParam(name = "currentPassword")String currentPassword, @RequestParam(name = "newPassword")String newPassword){
        log.info("[PASSWORD_CHANGE] Request | email={}", email);

        if (newPassword == null || newPassword.length() < 8){
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "New password must be at least 8 characters long"
            ));
        }

        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of(
                    "error" , "User not found"
            ));
        }
        Users user = userOptional.get();
        if (!passwordEncoder.matches(currentPassword , user.getPassword())){
            log.warn("[PASSWORD_CHANGE] Current password mismatch | email={}", email);
            return ResponseEntity.badRequest().body(Map.of(
                    "error" , "Current password is incorrect"
            ));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("[PASSWORD_CHANGE] Success | email = {} " , email);

        return ResponseEntity.ok(Map.of(
                "message" , "Password changed successfully"
        ));
    }


//    Helpers
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }

        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }

    private String formatToE164(String phone){
        if (phone == null || phone.isBlank()){
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        phone = phone.trim().replaceAll("[^\\d+]" , "");
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("91")) return "+" + phone;

        return "+91" + phone;
    }


    private String extractNumber(Object user){
        if (user instanceof Users u1){
            return u1.getPhoneNumber();
        }
        throw new IllegalArgumentException("Unsupported user type: " + user.getClass());
    }
}
