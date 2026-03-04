package com.skuri.skuri_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkuriBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkuriBackendApplication.class, args);
	}

}
