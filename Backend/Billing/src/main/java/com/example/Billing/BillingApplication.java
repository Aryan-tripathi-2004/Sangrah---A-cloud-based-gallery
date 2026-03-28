package com.example.Billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Billing Microservice Application
 *
 * Configuration is loaded from:
 * - DEV: application.properties + .env file (via java-dotenv, optional)
 * - PROD: System environment variables + application.properties
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class BillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingApplication.class, args);
	}

}
