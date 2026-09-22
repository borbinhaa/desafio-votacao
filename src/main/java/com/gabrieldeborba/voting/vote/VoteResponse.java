package com.gabrieldeborba.voting.vote;

import java.time.Instant;
import java.util.UUID;

/** The CPF is deliberately not echoed back. */
public record VoteResponse(UUID id, UUID agendaId, VoteChoice choice, Instant votedAt) {

    static VoteResponse from(Vote vote) {
        return new VoteResponse(vote.getId(), vote.getAgenda().getId(), vote.getChoice(), vote.getVotedAt());
    }
}
