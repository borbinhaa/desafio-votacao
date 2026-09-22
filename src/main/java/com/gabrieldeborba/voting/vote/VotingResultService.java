package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaService;
import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotingResultService {

    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final AgendaService agendaService;
    private final Clock clock;

    public VotingResultService(
            VoteRepository voteRepository,
            VotingSessionRepository sessionRepository,
            AgendaService agendaService,
            Clock clock) {
        this.voteRepository = voteRepository;
        this.sessionRepository = sessionRepository;
        this.agendaService = agendaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VotingResultResponse result(UUID agendaId) {
        Agenda agenda = agendaService.getAgenda(agendaId);
        VotingStatus status = sessionRepository
                .findByAgendaId(agendaId)
                .map(this::statusOf)
                .orElse(VotingStatus.NOT_OPENED);

        long yes = 0;
        long no = 0;
        for (VoteCount count : voteRepository.countByAgendaIdGroupedByChoice(agendaId)) {
            switch (count.choice()) {
                case YES -> yes = count.total();
                case NO -> no = count.total();
                default -> throw new IllegalStateException("Unhandled vote choice: " + count.choice());
            }
        }
        return new VotingResultResponse(
                agendaId, agenda.getTitle(), status, yes, no, yes + no, outcomeOf(status, yes, no));
    }

    private VotingStatus statusOf(VotingSession session) {
        return session.isOpen(clock.instant()) ? VotingStatus.OPEN : VotingStatus.CLOSED;
    }

    /** The outcome is only official after the session closes; before that it is null. */
    private static @Nullable VotingOutcome outcomeOf(VotingStatus status, long yes, long no) {
        if (status != VotingStatus.CLOSED) {
            return null;
        }
        if (yes == no) {
            return VotingOutcome.TIED;
        }
        return yes > no ? VotingOutcome.APPROVED : VotingOutcome.REJECTED;
    }
}
