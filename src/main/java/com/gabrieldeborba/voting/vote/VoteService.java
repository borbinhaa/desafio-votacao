package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.agenda.AgendaService;
import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionRepository;
import com.gabrieldeborba.voting.vote.dto.VoteRequest;
import com.gabrieldeborba.voting.vote.dto.VoteResponse;
import com.gabrieldeborba.voting.vote.exception.MemberAlreadyVotedException;
import com.gabrieldeborba.voting.vote.exception.VotingSessionClosedException;
import com.gabrieldeborba.voting.vote.exception.VotingSessionNotOpenException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final AgendaService agendaService;
    private final Clock clock;

    public VoteService(
            VoteRepository voteRepository,
            VotingSessionRepository sessionRepository,
            AgendaService agendaService,
            Clock clock) {
        this.voteRepository = voteRepository;
        this.sessionRepository = sessionRepository;
        this.agendaService = agendaService;
        this.clock = clock;
    }

    /**
     * Registers a vote. Happy path costs one select (the session) and one insert; the agenda is only
     * looked up when there is no session, to tell 404 (no agenda) from 422 (agenda without session).
     * Duplicate votes are detected by the unique constraint, not by a prior query, so concurrent
     * requests from the same member cannot both succeed.
     */
    @Transactional
    public VoteResponse castVote(UUID agendaId, VoteRequest request) {
        Optional<VotingSession> session = sessionRepository.findByAgendaId(agendaId);
        if (session.isEmpty()) {
            agendaService.getAgenda(agendaId); // 404 when the agenda itself does not exist
            throw new VotingSessionNotOpenException(agendaId);
        }
        Instant now = clock.instant();
        if (!session.get().isOpen(now)) {
            throw new VotingSessionClosedException(agendaId, session.get().getClosesAt());
        }

        Vote vote;
        try {
            vote = voteRepository.saveAndFlush(
                    new Vote(session.get().getAgenda(), request.cpf(), request.choice(), now));
        } catch (DataIntegrityViolationException e) {
            // Not chained on purpose: the driver message contains the CPF. See MemberAlreadyVotedException.
            throw new MemberAlreadyVotedException(agendaId);
        }
        log.info(
                "Vote registered: voteId={} agendaId={} choice={} cpf={}",
                vote.getId(),
                agendaId,
                vote.getChoice(),
                maskCpf(request.cpf()));
        return VoteResponse.from(vote);
    }

    /** Keeps only the check digits in logs: 12345678909 -> ***.***.***-09. */
    static String maskCpf(String cpf) {
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }
}
