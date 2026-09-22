package com.gabrieldeborba.voting.session.dto;

import com.gabrieldeborba.voting.session.VotingSession;
import com.gabrieldeborba.voting.session.VotingSessionStatus;
import java.time.Instant;
import java.util.UUID;

public record VotingSessionResponse(
        UUID id, UUID agendaId, Instant openedAt, Instant closesAt, VotingSessionStatus status) {

    public static VotingSessionResponse from(VotingSession session, Instant now) {
        return new VotingSessionResponse(
                session.getId(),
                session.getAgenda().getId(),
                session.getOpenedAt(),
                session.getClosesAt(),
                session.status(now));
    }
}
