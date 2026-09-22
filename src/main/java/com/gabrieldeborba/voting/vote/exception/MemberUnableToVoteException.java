package com.gabrieldeborba.voting.vote.exception;

import com.gabrieldeborba.voting.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/** The CPF exists but the external service says the member cannot vote. Mapped to HTTP 422. */
public class MemberUnableToVoteException extends DomainException {

    public MemberUnableToVoteException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Member is not allowed to vote");
    }
}
