package com.gabrieldeborba.voting.common.cpf.exception;

import com.gabrieldeborba.voting.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/** The CPF does not exist (bad check digits). The challenge mandates HTTP 404 for this case. */
public class InvalidCpfException extends DomainException {

    public InvalidCpfException() {
        super(HttpStatus.NOT_FOUND, "CPF is invalid");
    }
}
