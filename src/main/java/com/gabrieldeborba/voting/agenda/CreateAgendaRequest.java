package com.gabrieldeborba.voting.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateAgendaRequest(
        @NotBlank @Size(max = 200) String title, @Nullable @Size(max = 2000) String description) {}
