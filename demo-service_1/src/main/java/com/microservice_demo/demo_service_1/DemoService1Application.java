package com.microservice_demo.demo_service_1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DemoService1Application {

	public static void main(String[] args) {
		SpringApplication.run(DemoService1Application.class, args);
	}

}
