package com.thiefspin.bookingsystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Booking System API",
        version = "v1",
        description = "Appointment booking API for branch appointments"
    )
)
public class OpenApiConfig {

}
