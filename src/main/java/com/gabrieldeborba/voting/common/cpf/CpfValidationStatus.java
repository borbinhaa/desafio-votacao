package com.gabrieldeborba.voting.common.cpf;

/** Answer of the external CPF service, as specified by the challenge ({@code {"status": "ABLE_TO_VOTE"}}). */
public enum CpfValidationStatus {
    ABLE_TO_VOTE,
    UNABLE_TO_VOTE
}
