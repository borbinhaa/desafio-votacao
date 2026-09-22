package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** The agenda exists but no voting session was opened for it yet. Mapped to HTTP 422. */
public class VotingSessionNotOpenException extends DomainException {

    public VotingSessionNotOpenException(UUID agendaId) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Voting session for agenda " + agendaId + " has not been opened");
    }
}
