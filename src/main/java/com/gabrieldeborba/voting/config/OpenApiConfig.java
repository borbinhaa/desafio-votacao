package com.gabrieldeborba.voting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata shown at the top of Swagger UI ({@code /swagger-ui.html}); the operations come from the controllers. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI votingApi() {
        return new OpenAPI().info(new Info().title("Voting API").version("v1").description("""
                                REST API for cooperative assembly voting: register agendas, open a timed voting \
                                session, receive one YES/NO vote per member (identified by CPF) and read the tally.

                                Errors follow RFC 7807 (`application/problem+json`): 400 validation, 404 not found \
                                or invalid CPF, 409 conflict (session already open, member already voted), \
                                422 business rule (session not open or closed, member unable to vote). \
                                Every problem carries a `timestamp`; validation problems add `errors[{field, message}]`."""));
    }
}
