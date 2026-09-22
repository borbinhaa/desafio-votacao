package com.gabrieldeborba.voting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

class ApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void healthEndpointReportsUp() {
        client.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void openApiDocumentDescribesTheV1Endpoints() {
        client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.info.title")
                .isEqualTo("Voting API")
                .jsonPath("$.info.version")
                .isEqualTo("v1")
                .jsonPath("$.paths['/api/v1/agendas'].post.responses['201']")
                .exists()
                .jsonPath("$.paths['/api/v1/agendas/{agendaId}/votes'].post.responses['422']")
                .exists()
                .jsonPath("$.paths['/api/v1/agendas/{agendaId}/result'].get")
                .exists();
    }
}
