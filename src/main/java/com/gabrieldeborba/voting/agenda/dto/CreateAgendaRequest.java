package com.gabrieldeborba.voting.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateAgendaRequest(
        @Schema(example = "Approve the 2026 budget") @NotBlank @Size(max = 200)
        String title,

        @Schema(example = "Annual budget of the cooperative") @Nullable @Size(max = 2000)
        String description) {}
