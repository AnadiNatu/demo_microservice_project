package com.microservice_demo.demo_service_2.exception.errors;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String msg) {
        super(msg);
    }
}
