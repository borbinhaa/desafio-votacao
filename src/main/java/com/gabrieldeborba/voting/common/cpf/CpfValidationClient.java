package com.gabrieldeborba.voting.common.cpf;

import com.gabrieldeborba.voting.common.cpf.exception.InvalidCpfException;

/**
 * Facade for the external service that tells whether a member may vote. The only implementation in
 * this project is a fake ({@link FakeCpfValidationClient}); a real HTTP client would implement the
 * same contract without touching the vote flow.
 */
public interface CpfValidationClient {

    /**
     * @return ABLE_TO_VOTE or UNABLE_TO_VOTE
     * @throws InvalidCpfException when the CPF does not exist (invalid check digits) — HTTP 404, as
     *     the challenge mandates
     */
    CpfValidationStatus validate(String cpf);
}
