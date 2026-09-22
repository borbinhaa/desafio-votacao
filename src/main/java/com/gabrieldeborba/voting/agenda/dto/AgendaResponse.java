package com.gabrieldeborba.voting.agenda.dto;

import com.gabrieldeborba.voting.agenda.Agenda;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AgendaResponse(UUID id, String title, @Nullable String description, Instant createdAt) {

    public static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(
                agenda.getId(), agenda.getTitle(), agenda.getDescription(), agenda.getCreatedAt());
    }
}
