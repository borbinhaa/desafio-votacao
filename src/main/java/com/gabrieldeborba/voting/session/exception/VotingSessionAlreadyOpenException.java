package com.gabrieldeborba.voting.session.exception;

import com.gabrieldeborba.voting.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** An agenda can have only one voting session. Mapped to HTTP 409. */
public class VotingSessionAlreadyOpenException extends DomainException {

    public VotingSessionAlreadyOpenException(UUID agendaId, Throwable cause) {
        super(HttpStatus.CONFLICT, "Agenda " + agendaId + " already has a voting session", cause);
    }
}
