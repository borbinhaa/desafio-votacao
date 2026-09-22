package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Each member votes once per agenda. Mapped to HTTP 409.
 *
 * <p>Deliberately does not chain the database exception as its cause: PostgreSQL's unique-violation
 * message echoes the offending row, which would put the raw CPF into any stack trace.
 */
public class MemberAlreadyVotedException extends DomainException {

    public MemberAlreadyVotedException(UUID agendaId) {
        super(HttpStatus.CONFLICT, "Member has already voted on agenda " + agendaId);
    }
}
