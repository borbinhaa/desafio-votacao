package com.gabrieldeborba.voting.session.exception;

import com.gabrieldeborba.voting.common.exception.ResourceNotFoundException;
import java.util.UUID;

public class VotingSessionNotFoundException extends ResourceNotFoundException {

    public VotingSessionNotFoundException(UUID agendaId) {
        super("No voting session opened for agenda " + agendaId);
    }
}
