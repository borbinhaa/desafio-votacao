package com.gabrieldeborba.voting.result.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gabrieldeborba.voting.result.VotingOutcome;
import com.gabrieldeborba.voting.result.VotingStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Vote tally of an agenda. Counts are always present (partial while the session is open); the
 * {@code outcome} exists only once the session is closed, so it is omitted from the JSON otherwise.
 */
public record VotingResultResponse(
        UUID agendaId,
        String title,
        VotingStatus status,
        long yesVotes,
        long noVotes,
        long totalVotes,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable VotingOutcome outcome) {}
