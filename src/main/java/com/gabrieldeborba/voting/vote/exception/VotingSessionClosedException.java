package com.gabrieldeborba.voting.vote.exception;

import com.gabrieldeborba.voting.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** The voting session of the agenda has already ended. Mapped to HTTP 422. */
public class VotingSessionClosedException extends DomainException {

    public VotingSessionClosedException(UUID agendaId, Instant closedAt) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Voting session for agenda " + agendaId + " closed at " + closedAt);
    }
}
