package com.gt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GtPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(GtPlatformApplication.class, args);
	}

}
