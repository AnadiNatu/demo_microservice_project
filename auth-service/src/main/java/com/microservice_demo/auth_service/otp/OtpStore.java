package com.microservice_demo.auth_service.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;

//    Temp store of generated otp
    private record OtpEntry(String otp , LocalDateTime expireAt , int attempt , LocalDateTime lockedUntil){}

    private final Map<String , OtpEntry> store = new ConcurrentHashMap<>();

    public void save(String key , String otp){
       store.put(key , new OtpEntry(otp , LocalDateTime.now().plusMinutes(expiryMinutes) , 0 , null));
    }

    public boolean validate(String key , String otp){
        OtpEntry entry = store.get(key);
        if (entry == null) return false;
        LocalDateTime now = LocalDateTime.now();

        if (entry.lockedUntil() != null && now.isBefore(entry.lockedUntil())){
            return false;
        }

        if (now.isAfter(entry.expireAt())){
            store.remove(key);
            return false;
        }

        if (entry.otp().equals(otp)){
            store.remove(key);
            return true;
        }

        int newAttempt = entry.attempt() + 1;

        if (newAttempt >= MAX_ATTEMPTS){
            store.put(key , new OtpEntry(
                    entry.otp(),
                    entry.expireAt(),
                    newAttempt,
                    now.plusMinutes(LOCK_MINUTES)
            ));
        }else{
            store.put(key , new OtpEntry(
                    entry.otp(),
                    entry.expireAt(),
                    newAttempt,
                    null
            ));
        }
        return false;
    }


    public void invalidate(String key){
        store.remove(key);
    }
}
