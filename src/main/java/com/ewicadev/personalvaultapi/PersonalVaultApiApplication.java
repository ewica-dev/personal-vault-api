package com.ewicadev.personalvaultapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonalVaultApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalVaultApiApplication.class, args);
	}

}
