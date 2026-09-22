package com.gabrieldeborba.voting.session;

import com.gabrieldeborba.voting.AbstractIntegrationTest;
import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class VotingSessionIT extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Test
    void opensSessionOnceAndRejectsSecondAttempt() {
        UUID agendaId = createAgenda();

        client.post()
                .uri("/api/v1/agendas/{id}/session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"durationMinutes\":5}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.agendaId")
                .isEqualTo(agendaId.toString())
                .jsonPath("$.status")
                .isEqualTo("OPEN");

        client.post()
                .uri("/api/v1/agendas/{id}/session", agendaId)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Agenda " + agendaId + " already has a voting session");

        client.get()
                .uri("/api/v1/agendas/{id}/session", agendaId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("OPEN");
    }

    @Test
    void reportsClosedWhenClosesAtIsInThePast() {
        Agenda agenda = agendaRepository.save(new Agenda("Expired session", null, Instant.now()));
        Instant openedAt = Instant.now().minusSeconds(120);
        sessionRepository.save(new VotingSession(agenda, openedAt, openedAt.plusSeconds(60)));

        client.get()
                .uri("/api/v1/agendas/{id}/session", agenda.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CLOSED");
    }

    @Test
    void returns404WhenAgendaDoesNotExist() {
        client.post()
                .uri("/api/v1/agendas/{id}/session", UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void returns404WhenAgendaHasNoSession() {
        UUID agendaId = createAgenda();

        client.get()
                .uri("/api/v1/agendas/{id}/session", agendaId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("No voting session opened for agenda " + agendaId);
    }

    private UUID createAgenda() {
        return agendaRepository
                .save(new Agenda("Session test", null, Instant.now()))
                .getId();
    }
}
