package com.gabrieldeborba.voting.agenda;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AgendaResponse(UUID id, String title, @Nullable String description, Instant createdAt) {

    static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(
                agenda.getId(), agenda.getTitle(), agenda.getDescription(), agenda.getCreatedAt());
    }
}
