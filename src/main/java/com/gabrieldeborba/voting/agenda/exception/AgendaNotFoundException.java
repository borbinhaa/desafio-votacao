package com.gabrieldeborba.voting.agenda.exception;

import com.gabrieldeborba.voting.common.exception.ResourceNotFoundException;
import java.util.UUID;

public class AgendaNotFoundException extends ResourceNotFoundException {

    public AgendaNotFoundException(UUID agendaId) {
        super("Agenda " + agendaId + " not found");
    }
}
