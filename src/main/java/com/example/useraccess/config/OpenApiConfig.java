package com.example.useraccess.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI userAccessOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("User Access Service API").description("APIs for user summary and permissions")
						.version("v1").contact(new Contact().name("Sandeep Gajjala"))
						.license(new License().name("Internal Use")))
				.externalDocs(new ExternalDocumentation().description("Project Notes"));
	}
}