package com.example.Auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Sangrah Auth Service.
 * 
 * This microservice handles authentication and authorization for the Sangrah
 * cloud storage platform. It provides:
 * - User registration and email verification
 * - Username/password authentication
 * - OAuth2 login (Google, Microsoft)
 * - JWT token generation and validation
 * - Session management
 * - Password reset flows
 * - Profile management
 * - Role-based access control
 * 
 * Now using MySQL with JPA/Hibernate for data persistence.
 */

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.example.Auth.repository")
@EntityScan(basePackages = "com.example.Auth.entity")
@EnableTransactionManagement
@EnableAsync
@ComponentScan(basePackages = {
		"com.example.Auth",
		"com.example.common"
})
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

}
