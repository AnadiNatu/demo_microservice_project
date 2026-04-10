package com.microservice_demo.auth_service.notifcation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;

    public void sendDualChannelOtp(String email , String phone){
        log.info("Sending dual-channel OTP | email={} | phone = {} " , email, phone);
        emailService.sendOtpViaEmail(email);
        smsService.sendOtpViaSms(phone);
    }

    public void sendWelcomeNotification(String email, String phone, String firstName) {
        log.info("Sending welcome notification | email={} | phone={}", email, phone);
        emailService.sendWelcomeEmail(email, firstName);
        smsService.sendSms(phone, "Welcome " + firstName + "! Your account on MultiUser Platform has been created.");
    }

    public void sendPasswordResetOtp(String email) {
        log.info("Sending password reset OTP | email={}", email);
        emailService.sendOtpViaEmail(email);
    }

    public void sendLoginAlert(String phone, String firstName) {
        log.info("Sending login alert SMS | phone={}", phone);
        smsService.sendSms(phone, "Hi " + firstName + ", a new login to your MultiUser account was detected. If this wasn't you, contact support immediately.");
    }
}
