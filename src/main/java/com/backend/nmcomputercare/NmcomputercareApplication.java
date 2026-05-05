package com.backend.nmcomputercare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class NmcomputercareApplication {

	public static void main(String[] args) {
		SpringApplication.run(NmcomputercareApplication.class, args);
	}

}
