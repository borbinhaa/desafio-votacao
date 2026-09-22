package com.gabrieldeborba.voting.vote.dto;

import com.gabrieldeborba.voting.vote.VoteChoice;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The member is identified by CPF (11 digits, no punctuation). */
public record VoteRequest(
        @NotNull @Pattern(regexp = "\\d{11}", message = "must contain exactly 11 digits") String cpf,
        @NotNull VoteChoice choice) {}
