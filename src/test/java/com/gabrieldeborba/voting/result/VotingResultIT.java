package com.gabrieldeborba.voting.result;

import com.gabrieldeborba.voting.AbstractIntegrationTest;
import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaRepository;
import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class VotingResultIT extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Test
    void countsVotesCastThroughTheApi() {
        Agenda agenda = agendaRepository.save(new Agenda("Result test", null, Instant.now()));
        sessionRepository.save(
                new VotingSession(agenda, Instant.now(), Instant.now().plusSeconds(600)));
        UUID agendaId = agenda.getId();

        vote(agendaId, "12345678909", "YES");
        vote(agendaId, "98765432100", "YES");
        vote(agendaId, "11122233396", "NO");

        client.get()
                .uri("/api/v1/agendas/{id}/result", agendaId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Result test")
                .jsonPath("$.status")
                .isEqualTo("OPEN")
                .jsonPath("$.yesVotes")
                .isEqualTo(2)
                .jsonPath("$.noVotes")
                .isEqualTo(1)
                .jsonPath("$.totalVotes")
                .isEqualTo(3)
                .jsonPath("$.outcome")
                .doesNotExist();
    }

    @Test
    void reportsOutcomeForClosedSession() {
        Agenda agenda = agendaRepository.save(new Agenda("Closed result", null, Instant.now()));
        sessionRepository.save(new VotingSession(
                agenda, Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)));

        client.get()
                .uri("/api/v1/agendas/{id}/result", agenda.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CLOSED")
                .jsonPath("$.totalVotes")
                .isEqualTo(0)
                .jsonPath("$.outcome")
                .isEqualTo("TIED");
    }

    @Test
    void agendaWithoutSessionIsNotOpened() {
        Agenda agenda = agendaRepository.save(new Agenda("No session", null, Instant.now()));

        client.get()
                .uri("/api/v1/agendas/{id}/result", agenda.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("NOT_OPENED")
                .jsonPath("$.outcome")
                .doesNotExist();
    }

    @Test
    void returns404ForUnknownAgenda() {
        client.get()
                .uri("/api/v1/agendas/{id}/result", UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private void vote(UUID agendaId, String cpf, String choice) {
        client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"" + cpf + "\",\"choice\":\"" + choice + "\"}")
                .exchange()
                .expectStatus()
                .isCreated();
    }
}
