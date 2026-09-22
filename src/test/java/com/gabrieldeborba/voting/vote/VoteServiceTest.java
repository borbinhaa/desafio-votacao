package com.gabrieldeborba.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaService;
import com.gabrieldeborba.voting.agenda.exception.AgendaNotFoundException;
import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionRepository;
import com.gabrieldeborba.voting.vote.dto.VoteRequest;
import com.gabrieldeborba.voting.vote.dto.VoteResponse;
import com.gabrieldeborba.voting.vote.exception.MemberAlreadyVotedException;
import com.gabrieldeborba.voting.vote.exception.VotingSessionClosedException;
import com.gabrieldeborba.voting.vote.exception.VotingSessionNotOpenException;
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
class VoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final VoteRequest YES_VOTE = new VoteRequest("12345678909", VoteChoice.YES);

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VotingSessionRepository sessionRepository;

    @Mock
    private AgendaService agendaService;

    private VoteService service;
    private Agenda agenda;

    @BeforeEach
    void setUp() {
        service = new VoteService(voteRepository, sessionRepository, agendaService, Clock.fixed(NOW, ZoneOffset.UTC));
        agenda = new Agenda("Budget 2026", null, NOW);
        ReflectionTestUtils.setField(agenda, "id", AGENDA_ID);
    }

    @Test
    void castVoteSavesWhenSessionIsOpen() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(openSession()));
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> inv.getArgument(0));

        VoteResponse response = service.castVote(AGENDA_ID, YES_VOTE);

        ArgumentCaptor<Vote> saved = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAgenda()).isSameAs(agenda);
        assertThat(saved.getValue().getMemberCpf()).isEqualTo("12345678909");
        assertThat(saved.getValue().getChoice()).isEqualTo(VoteChoice.YES);
        assertThat(saved.getValue().getVotedAt()).isEqualTo(NOW);
        assertThat(response.agendaId()).isEqualTo(AGENDA_ID);
        assertThat(response.choice()).isEqualTo(VoteChoice.YES);
        verifyNoInteractions(agendaService);
    }

    @Test
    void castVoteRejectsClosedSession() {
        VotingSession closed = new VotingSession(agenda, NOW.minusSeconds(120), NOW.minusSeconds(60));
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.castVote(AGENDA_ID, YES_VOTE))
                .isInstanceOf(VotingSessionClosedException.class)
                .hasMessage("Voting session for agenda " + AGENDA_ID + " closed at " + NOW.minusSeconds(60));
        verify(voteRepository, never()).saveAndFlush(any());
    }

    @Test
    void castVoteRejectsSessionClosingExactlyNow() {
        VotingSession closingNow = new VotingSession(agenda, NOW.minusSeconds(60), NOW);
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(closingNow));

        assertThatThrownBy(() -> service.castVote(AGENDA_ID, YES_VOTE)).isInstanceOf(VotingSessionClosedException.class);
    }

    @Test
    void castVoteFailsWith422WhenAgendaHasNoSession() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.empty());
        when(agendaService.getAgenda(AGENDA_ID)).thenReturn(agenda);

        assertThatThrownBy(() -> service.castVote(AGENDA_ID, YES_VOTE))
                .isInstanceOf(VotingSessionNotOpenException.class)
                .hasMessage("Voting session for agenda " + AGENDA_ID + " has not been opened");
    }

    @Test
    void castVoteFailsWith404WhenAgendaDoesNotExist() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.empty());
        when(agendaService.getAgenda(AGENDA_ID)).thenThrow(new AgendaNotFoundException(AGENDA_ID));

        assertThatThrownBy(() -> service.castVote(AGENDA_ID, YES_VOTE)).isInstanceOf(AgendaNotFoundException.class);
        verify(voteRepository, never()).saveAndFlush(any());
    }

    @Test
    void castVoteTranslatesUniqueViolationInto409() {
        when(sessionRepository.findByAgendaId(AGENDA_ID)).thenReturn(Optional.of(openSession()));
        when(voteRepository.saveAndFlush(any(Vote.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.castVote(AGENDA_ID, YES_VOTE))
                .isInstanceOf(MemberAlreadyVotedException.class)
                .hasMessage("Member has already voted on agenda " + AGENDA_ID)
                .hasNoCause();
    }

    @Test
    void maskCpfKeepsOnlyCheckDigits() {
        assertThat(VoteService.maskCpf("12345678909")).isEqualTo("***.***.***-09");
    }

    private VotingSession openSession() {
        return new VotingSession(agenda, NOW.minusSeconds(30), NOW.plusSeconds(30));
    }
}
