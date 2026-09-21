package com.gabrieldeborba.voting.session;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.agenda.AgendaService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotingSessionService {

    private static final Logger log = LoggerFactory.getLogger(VotingSessionService.class);

    private final VotingSessionRepository repository;
    private final AgendaService agendaService;
    private final Clock clock;

    public VotingSessionService(VotingSessionRepository repository, AgendaService agendaService, Clock clock) {
        this.repository = repository;
        this.agendaService = agendaService;
        this.clock = clock;
    }

    /**
     * Opens the single voting session of an agenda. Uniqueness is enforced by the database: a
     * concurrent second call fails on the unique constraint and is reported as 409.
     */
    @Transactional
    public VotingSessionResponse open(UUID agendaId, OpenVotingSessionRequest request) {
        Agenda agenda = agendaService.getAgenda(agendaId);
        Instant now = clock.instant();
        Instant closesAt = now.plus(Duration.ofMinutes(request.durationMinutesOrDefault()));

        VotingSession session;
        try {
            session = repository.saveAndFlush(new VotingSession(agenda, now, closesAt));
        } catch (DataIntegrityViolationException e) {
            throw new VotingSessionAlreadyOpenException(agendaId, e);
        }
        log.info(
                "Voting session opened: sessionId={} agendaId={} durationMinutes={} closesAt={}",
                session.getId(),
                agendaId,
                request.durationMinutesOrDefault(),
                closesAt);
        return VotingSessionResponse.from(session, now);
    }

    @Transactional(readOnly = true)
    public VotingSessionResponse findByAgendaId(UUID agendaId) {
        return VotingSessionResponse.from(getSession(agendaId), clock.instant());
    }

    /**
     * Loads the entity for other services (vote); throws 404 when the agenda has no session.
     *
     * <p>{@code agenda} is lazy and {@code open-in-view} is off: callers must run inside their own
     * transaction if they need anything beyond {@code getAgenda().getId()}.
     */
    @Transactional(readOnly = true)
    public VotingSession getSession(UUID agendaId) {
        return repository.findByAgendaId(agendaId).orElseThrow(() -> new VotingSessionNotFoundException(agendaId));
    }
}
