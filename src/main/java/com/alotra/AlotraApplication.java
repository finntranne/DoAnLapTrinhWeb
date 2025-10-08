package com.alotra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
public class AlotraApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlotraApplication.class, args);
	}

}
