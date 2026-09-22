package com.gabrieldeborba.voting;

import com.gabrieldeborba.voting.common.cpf.CpfValidationClient;
import com.gabrieldeborba.voting.common.cpf.CpfValidationStatus;
import com.gabrieldeborba.voting.common.cpf.FakeCpfValidationClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Integration tests replace the external CPF service with a deterministic answer: check digits are
 * still validated by the real fake (404 path), but every valid CPF is able to vote.
 */
@TestConfiguration(proxyBeanMethods = false)
class AlwaysAbleCpfClientConfiguration {

    @Bean
    @Primary
    CpfValidationClient alwaysAbleCpfValidationClient(FakeCpfValidationClient fake) {
        return cpf -> {
            fake.validate(cpf);
            return CpfValidationStatus.ABLE_TO_VOTE;
        };
    }
}
