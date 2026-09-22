package com.gabrieldeborba.voting.common.cpf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gabrieldeborba.voting.common.cpf.exception.InvalidCpfException;
import jakarta.validation.Validation;
import java.util.EnumSet;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FakeCpfValidationClientTest {

    private final FakeCpfValidationClient client = new FakeCpfValidationClient(
            Validation.buildDefaultValidatorFactory().getValidator());

    @ParameterizedTest
    @ValueSource(strings = {"12345678909", "98765432100", "11122233396", "52998224725", "00000000191"})
    void acceptsValidCheckDigits(String cpf) {
        assertThatCode(() -> client.validate(cpf)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11111111111", "00000000000", "12345678900", "12345678919", "11122233344", "123"})
    void rejectsWrongCheckDigitsWith404Exception(String cpf) {
        assertThatThrownBy(() -> client.validate(cpf)).isInstanceOf(InvalidCpfException.class);
    }

    @Test
    void answersAtRandomForValidCpf() {
        var seen = EnumSet.noneOf(CpfValidationStatus.class);
        IntStream.range(0, 200).forEach(i -> seen.add(client.validate("12345678909")));

        assertThat(seen)
                .containsExactlyInAnyOrder(CpfValidationStatus.ABLE_TO_VOTE, CpfValidationStatus.UNABLE_TO_VOTE);
    }
}
