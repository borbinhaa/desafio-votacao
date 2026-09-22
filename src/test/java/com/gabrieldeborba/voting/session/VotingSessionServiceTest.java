package com.gabrieldeborba.voting.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaService;
import com.gabrieldeborba.voting.agenda.exception.AgendaNotFoundException;
import com.gabrieldeborba.voting.session.dto.OpenVotingSessionRequest;
import com.gabrieldeborba.voting.session.dto.VotingSessionResponse;
import com.gabrieldeborba.voting.session.exception.VotingSessionAlreadyOpenException;
import com.gabrieldeborba.voting.session.exception.VotingSessionNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VotingSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private VotingSessionRepository repository;

    @Mock
    private AgendaService agendaService;

    private VotingSessionService service;
    private Agenda agenda;

    @BeforeEach
    void setUp() {
        service = new VotingSessionService(repository, agendaService, Clock.fixed(NOW, ZoneOffset.UTC));
        agenda = new Agenda("Budget 2026", null, NOW);
        ReflectionTestUtils.setField(agenda, "id", AGENDA_ID);
    }

    @Test
    void openDefaultsToOneMinute() {
        when(agendaService.getAgenda(AGENDA_ID)).thenReturn(agenda);
        when(repository.saveAndFlush(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSessionResponse response = service.open(AGENDA_ID, new OpenVotingSessionRequest(null));

        assertThat(response.agendaId()).isEqualTo(AGENDA_ID);
        assertThat(response.openedAt()).isEqualTo(NOW);
        assertThat(response.closesAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(response.status()).isEqualTo(VotingSessionStatus.OPEN);
    }

    @Test
    void openUsesRequestedDuration() {
        when(agendaService.getAgenda(AGENDA_ID)).thenReturn(agenda);
        when(repository.saveAndFlush(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSessionResponse response = service.open(AGENDA_ID, new OpenVotingSessionRequest(15));

        ArgumentCaptor<VotingSession> saved = ArgumentCaptor.forClass(VotingSession.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAgenda()).isSameAs(agenda);
        assertThat(response.closesAt()).isEqualTo(NOW.plusSeconds(15 * 60));
    }

    @Test
    void openFailsWith404WhenAgendaDoesNotExist() {
        when(agendaService.getAgenda(AGENDA_ID)).thenThrow(new AgendaNotFoundException(AGENDA_ID));

        assertThatThrownBy(() -> service.open(AGENDA_ID, new OpenVotingSessionRequest(null)))
                .isInstanceOf(AgendaNotFoundException.class);
    }

    @Test
    void openTranslatesUniqueViolationInto409() {
        when(agendaService.getAgenda(AGENDA_ID)).thenReturn(agenda);
        when(repository.saveAndFlush(any(VotingSession.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.open(AGENDA_ID, new OpenVotingSessionRequest(null)))
                .isInstanceOf(VotingSessionAlreadyOpenException.class)
                .hasMessage("Agenda " + AGENDA_ID + " already has a voting session")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findReportsClosedOnceClosesAtHasPassed() {
        VotingSession session = new VotingSession(agenda, NOW.minusSeconds(120), NOW.minusSeconds(60));
        when(repository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session));

        VotingSessionResponse response = service.findByAgendaId(AGENDA_ID);

        assertThat(response.status()).isEqualTo(VotingSessionStatus.CLOSED);
    }

    @Test
    void findReportsOpenWhileClosesAtIsInTheFuture() {
        VotingSession session = new VotingSession(agenda, NOW, NOW.plusSeconds(1));
        when(repository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session));

        assertThat(service.findByAgendaId(AGENDA_ID).status()).isEqualTo(VotingSessionStatus.OPEN);
    }

    @Test
    void sessionIsClosedExactlyAtClosesAt() {
        VotingSession session = new VotingSession(agenda, NOW.minusSeconds(60), NOW);
        when(repository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session));

        assertThat(service.findByAgendaId(AGENDA_ID).status()).isEqualTo(VotingSessionStatus.CLOSED);
    }

    @Test
    void findFailsWith404WhenNoSession() {
        when(repository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByAgendaId(AGENDA_ID))
                .isInstanceOf(VotingSessionNotFoundException.class)
                .hasMessage("No voting session opened for agenda " + AGENDA_ID);
    }
}
