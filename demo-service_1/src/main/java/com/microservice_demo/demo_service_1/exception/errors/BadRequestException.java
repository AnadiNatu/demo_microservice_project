package com.microservice_demo.demo_service_1.exception.errors;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String msg) {
        super(msg);
    }
}
