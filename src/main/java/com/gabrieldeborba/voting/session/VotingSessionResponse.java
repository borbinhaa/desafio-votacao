package com.gabrieldeborba.voting.session;

import java.time.Instant;
import java.util.UUID;

public record VotingSessionResponse(
        UUID id, UUID agendaId, Instant openedAt, Instant closesAt, VotingSessionStatus status) {

    static VotingSessionResponse from(VotingSession session, Instant now) {
        return new VotingSessionResponse(
                session.getId(),
                session.getAgenda().getId(),
                session.getOpenedAt(),
                session.getClosesAt(),
                session.status(now));
    }
}
