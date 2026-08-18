package com.example.EurekaRegistrySevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaRegistryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaRegistryServiceApplication.class, args);
	}

}
