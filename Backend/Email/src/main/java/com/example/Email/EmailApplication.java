package com.example.Email;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EmailApplication {

	public static void main(String[] args) {
		// Load .env file BEFORE Spring initializes
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// Load all variables into System properties
		dotenv.entries().forEach(entry ->
			System.setProperty(entry.getKey(), entry.getValue())
		);

		System.out.println("✅ .env file loaded before Spring initialization");

		SpringApplication.run(EmailApplication.class, args);
	}

}
