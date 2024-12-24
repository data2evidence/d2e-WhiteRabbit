package com.arcadia.whiteRabbitService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class WhiteRabbitServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WhiteRabbitServiceApplication.class, args);
	}
}
