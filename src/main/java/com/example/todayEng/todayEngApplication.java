package com.example.todayEng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class todayEngApplication {

	public static void main(String[] args) {
		SpringApplication.run(todayEngApplication.class, args);
	}

}
