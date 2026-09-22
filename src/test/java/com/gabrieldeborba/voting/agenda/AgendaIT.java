package com.gabrieldeborba.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import com.gabrieldeborba.voting.AbstractIntegrationTest;
import com.gabrieldeborba.voting.agenda.dto.AgendaResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class AgendaIT extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void createsAndFetchesAgenda() {
        AgendaResponse created = client.post()
                .uri("/api/v1/agendas")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Approve 2026 budget\",\"description\":\"Annual budget\"}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .exists("Location")
                .expectBody(AgendaResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isNotNull();

        client.get()
                .uri("/api/v1/agendas/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Approve 2026 budget")
                .jsonPath("$.description")
                .isEqualTo("Annual budget");

        client.get()
                .uri("/api/v1/agendas?size=5")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[?(@.id == '" + created.id() + "')].title")
                .isEqualTo("Approve 2026 budget");
    }

    @Test
    void returns404ForUnknownAgenda() {
        UUID unknownId = UUID.randomUUID();
        client.get()
                .uri("/api/v1/agendas/{id}", unknownId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Agenda " + unknownId + " not found");
    }
}
