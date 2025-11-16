package com.bookingservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
	info = @Info(
		title = "Booking Service API",
		version = "v1",
		description = "REST API for bookings and property blocks"
	),
	servers = {
		@Server(url = "http://localhost:8080", description = "Local")
	}
)
public class OpenApiConfig {

	@Bean
	public GroupedOpenApi bookingsApi() {
		return GroupedOpenApi.builder()
			.group("bookings")
			.pathsToMatch("/api/bookings/**")
			.addOperationCustomizer((operation, handlerMethod) -> {
				operation.addParametersItem(
					new io.swagger.v3.oas.models.parameters.Parameter()
						.name("lang")
						.in("query")
						.description("Language (en_US, pt_BR)")
						.required(false)
						.example("en_US")
				);
				return operation;
			})
			.build();
	}

	@Bean
	public GroupedOpenApi blocksApi() {
		return GroupedOpenApi.builder()
			.group("blocks")
			.pathsToMatch("/api/blocks/**")
			.addOperationCustomizer((operation, handlerMethod) -> {
				operation.addParametersItem(
					new io.swagger.v3.oas.models.parameters.Parameter()
						.name("lang")
						.in("query")
						.description("Language (en_US, pt_BR)")
						.required(false)
						.example("en_US")
				);
				return operation;
			})
			.build();
	}
}


