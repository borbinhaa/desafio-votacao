package com.gabrieldeborba.voting.vote.dto;

import com.gabrieldeborba.voting.vote.VoteChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The member is identified by CPF (11 digits, no punctuation). */
public record VoteRequest(
        @Schema(description = "Member CPF, 11 digits, no punctuation", example = "12345678909")
        @NotNull
        @Pattern(regexp = "\\d{11}", message = "must contain exactly 11 digits")
        String cpf,

        @Schema(example = "YES") @NotNull VoteChoice choice) {}
