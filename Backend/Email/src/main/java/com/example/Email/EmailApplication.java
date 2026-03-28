package com.example.Email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Email Notification Microservice Application
 *
 * Configuration is loaded from:
 * - DEV: application.properties + .env file (via java-dotenv, optional)
 * - PROD: System environment variables + application.properties
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailApplication.class, args);
	}

}