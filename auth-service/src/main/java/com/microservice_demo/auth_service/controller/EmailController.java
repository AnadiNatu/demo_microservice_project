package com.microservice_demo.auth_service.controller;


import com.microservice_demo.auth_service.notifcation.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send/simple")
    public ResponseEntity<Map<String, Object>> sendSimpleEmail(
            @RequestBody Map<String, String> request) {

        String to      = request.get("to");
        String subject = request.get("subject");
        String body    = request.get("body");

        if (to == null || subject == null || body == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Fields 'to', 'subject' and 'body' are required"
            ));
        }

        log.info("[EMAIL] Simple email request | to={} | subject={}", to, subject);
        emailService.sendSimpleEmail(to, subject, body);

        return ResponseEntity.ok(Map.of(
                "message", "Email sent successfully",
                "to",      to
        ));
    }

    @PostMapping("/send/html")
    public ResponseEntity<Map<String, Object>> sendHtmlEmail(
            @RequestBody Map<String, String> request) {

        String to       = request.get("to");
        String subject  = request.get("subject");
        String htmlBody = request.get("htmlBody");

        if (to == null || subject == null || htmlBody == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Fields 'to', 'subject' and 'htmlBody' are required"
            ));
        }

        log.info("[EMAIL] HTML email request | to={} | subject={}", to, subject);
        emailService.sendHtmlEmail(to, subject, htmlBody);

        return ResponseEntity.ok(Map.of(
                "message", "HTML email sent successfully",
                "to",      to
        ));
    }

    @PostMapping("/send/welcome")
    public ResponseEntity<Map<String, Object>> sendWelcomeEmail(
            @RequestBody Map<String, String> request) {

        String to        = request.get("to");
        String firstName = request.get("firstName");

        if (to == null || firstName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Fields 'to' and 'firstName' are required"
            ));
        }

        log.info("[EMAIL] Welcome email request | to={}", to);
        emailService.sendWelcomeEmail(to, firstName);

        return ResponseEntity.ok(Map.of(
                "message", "Welcome email sent successfully",
                "to",      to
        ));
    }

    @PostMapping("/send/otp")
    public ResponseEntity<Map<String, Object>> sendOtpEmail(
            @RequestBody Map<String, String> request) {

        String to  = request.get("to");
        String otp = request.get("otp");

        if (to == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Fields 'to' and 'otp' are required"
            ));
        }

        log.info("[EMAIL] OTP email (external otp) request | to={}", to);
        emailService.sendOtpEmail(to, otp);

        return ResponseEntity.ok(Map.of(
                "message", "OTP email sent successfully",
                "to",      to
        ));
    }
}
