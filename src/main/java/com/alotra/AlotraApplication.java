package com.alotra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@SpringBootApplication
public class AlotraApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlotraApplication.class, args);
	}

	@Bean
	public Cloudinary cloudinary() {
		Cloudinary  c = new Cloudinary(ObjectUtils.asMap(
				"cloud_name", "drtilyfon",
				"api_key", "237936128632937",
				"api_secret", "VvXArXnzWeaTqsJlOXSY6aSGuec",
				"secure", true
				));
		return c;
	}
}
