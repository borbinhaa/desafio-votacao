package com.gabrieldeborba.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

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

class VoteIT extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Test
    void registersOneVotePerMemberAndRejectsDuplicates() {
        UUID agendaId = agendaWithSession(Instant.now().plusSeconds(600));

        VoteResponse created = client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(VoteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.agendaId()).isEqualTo(agendaId);
        assertThat(created.choice()).isEqualTo(VoteChoice.YES);
        assertThat(voteRepository.findById(created.id())).isPresent();

        client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"12345678909\",\"choice\":\"NO\"}")
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Member has already voted on agenda " + agendaId);

        client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"98765432100\",\"choice\":\"NO\"}")
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    void sameMemberCanVoteOnDifferentAgendas() {
        UUID first = agendaWithSession(Instant.now().plusSeconds(600));
        UUID second = agendaWithSession(Instant.now().plusSeconds(600));

        for (UUID agendaId : new UUID[] {first, second}) {
            client.post()
                    .uri("/api/v1/agendas/{id}/votes", agendaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}")
                    .exchange()
                    .expectStatus()
                    .isCreated();
        }
    }

    @Test
    void rejectsVoteAfterSessionClosed() {
        UUID agendaId = agendaWithSession(Instant.now().minusSeconds(1));

        client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}")
                .exchange()
                .expectStatus()
                .isEqualTo(422);
    }

    @Test
    void rejectsVoteWhenAgendaHasNoSession() {
        UUID agendaId = agendaRepository.save(new Agenda("No session", null, Instant.now())).getId();

        client.post()
                .uri("/api/v1/agendas/{id}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}")
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Voting session for agenda " + agendaId + " has not been opened");
    }

    @Test
    void returns404WhenAgendaDoesNotExist() {
        client.post()
                .uri("/api/v1/agendas/{id}/votes", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private UUID agendaWithSession(Instant closesAt) {
        Agenda agenda = agendaRepository.save(new Agenda("Vote test", null, Instant.now()));
        sessionRepository.save(new VotingSession(agenda, closesAt.minusSeconds(60), closesAt));
        return agenda.getId();
    }
}
