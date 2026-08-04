package com.example.todayEng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class todayEngApplication {

	public static void main(String[] args) {
		SpringApplication.run(todayEngApplication.class, args);
	}

}
