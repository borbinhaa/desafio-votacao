package com.gabrieldeborba.voting.common.cpf;

import com.gabrieldeborba.voting.common.cpf.exception.InvalidCpfException;
import jakarta.validation.Validator;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the external CPF service required by the challenge: rejects CPFs with wrong check
 * digits (Hibernate Validator's {@link CPF} rule) and answers ABLE_TO_VOTE / UNABLE_TO_VOTE at random
 * for valid ones.
 */
@Component
public class FakeCpfValidationClient implements CpfValidationClient {

    /** Carrier for the {@link CPF} constraint so it can be checked programmatically, not on the request. */
    private record CpfValue(@CPF String value) {}

    private final Validator validator;

    public FakeCpfValidationClient(Validator validator) {
        this.validator = validator;
    }

    @Override
    public CpfValidationStatus validate(String cpf) {
        if (!validator.validateValue(CpfValue.class, "value", cpf).isEmpty()) {
            throw new InvalidCpfException();
        }
        // The challenge asks for a random answer: the same CPF may pass in one call and fail in the next.
        return Math.random() < 0.5 ? CpfValidationStatus.ABLE_TO_VOTE : CpfValidationStatus.UNABLE_TO_VOTE;
    }
}
