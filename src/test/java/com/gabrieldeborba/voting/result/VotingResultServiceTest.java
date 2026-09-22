package com.gabrieldeborba.voting.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaService;
import com.gabrieldeborba.voting.agenda.exception.AgendaNotFoundException;
import com.gabrieldeborba.voting.result.dto.VotingResultResponse;
import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionRepository;
import com.gabrieldeborba.voting.vote.VoteChoice;
import com.gabrieldeborba.voting.vote.VoteCount;
import com.gabrieldeborba.voting.vote.VoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VotingResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VotingSessionRepository sessionRepository;

    @Mock
    private AgendaService agendaService;

    private VotingResultService service;
    private Agenda agenda;

    @BeforeEach
    void setUp() {
        service = new VotingResultService(
                voteRepository, sessionRepository, agendaService, Clock.fixed(NOW, ZoneOffset.UTC));
        agenda = new Agenda("Budget 2026", null, NOW);
        ReflectionTestUtils.setField(agenda, "id", AGENDA_ID);
        when(agendaService.getAgenda(AGENDA_ID)).thenReturn(agenda);
    }

    @Test
    void agendaWithoutSessionHasZeroCountsAndNoOutcome() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.empty());
        when(voteRepository.countByAgendaIdGroupedByChoice(AGENDA_ID)).thenReturn(List.of());

        VotingResultResponse result = service.result(AGENDA_ID);

        assertThat(result.title()).isEqualTo("Budget 2026");
        assertThat(result.status()).isEqualTo(VotingStatus.NOT_OPENED);
        assertThat(result.yesVotes()).isZero();
        assertThat(result.noVotes()).isZero();
        assertThat(result.totalVotes()).isZero();
        assertThat(result.outcome()).isNull();
    }

    @Test
    void openSessionReportsPartialCountsWithoutOutcome() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session(NOW.plusSeconds(60))));
        when(voteRepository.countByAgendaIdGroupedByChoice(AGENDA_ID))
                .thenReturn(List.of(new VoteCount(VoteChoice.YES, 3), new VoteCount(VoteChoice.NO, 1)));

        VotingResultResponse result = service.result(AGENDA_ID);

        assertThat(result.status()).isEqualTo(VotingStatus.OPEN);
        assertThat(result.yesVotes()).isEqualTo(3);
        assertThat(result.noVotes()).isEqualTo(1);
        assertThat(result.totalVotes()).isEqualTo(4);
        assertThat(result.outcome()).isNull();
    }

    @ParameterizedTest(name = "yes={0} no={1} -> {2}")
    @CsvSource({"5, 2, APPROVED", "2, 5, REJECTED", "3, 3, TIED", "0, 0, TIED", "1, 0, APPROVED"})
    void closedSessionReportsOutcome(long yes, long no, VotingOutcome expected) {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session(NOW.minusSeconds(1))));
        when(voteRepository.countByAgendaIdGroupedByChoice(AGENDA_ID)).thenReturn(counts(yes, no));

        VotingResultResponse result = service.result(AGENDA_ID);

        assertThat(result.status()).isEqualTo(VotingStatus.CLOSED);
        assertThat(result.yesVotes()).isEqualTo(yes);
        assertThat(result.noVotes()).isEqualTo(no);
        assertThat(result.outcome()).isEqualTo(expected);
    }

    @Test
    void missingChoiceInAggregateCountsAsZero() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(session(NOW.minusSeconds(1))));
        when(voteRepository.countByAgendaIdGroupedByChoice(AGENDA_ID))
                .thenReturn(List.of(new VoteCount(VoteChoice.NO, 2)));

        VotingResultResponse result = service.result(AGENDA_ID);

        assertThat(result.yesVotes()).isZero();
        assertThat(result.noVotes()).isEqualTo(2);
        assertThat(result.outcome()).isEqualTo(VotingOutcome.REJECTED);
    }

    @Test
    void failsWith404WhenAgendaDoesNotExist() {
        when(agendaService.getAgenda(AGENDA_ID)).thenThrow(new AgendaNotFoundException(AGENDA_ID));

        assertThatThrownBy(() -> service.result(AGENDA_ID)).isInstanceOf(AgendaNotFoundException.class);
    }

    private VotingSession session(Instant closesAt) {
        return new VotingSession(agenda, closesAt.minusSeconds(60), closesAt);
    }

    private static List<VoteCount> counts(long yes, long no) {
        return List.of(new VoteCount(VoteChoice.YES, yes), new VoteCount(VoteChoice.NO, no));
    }
}
