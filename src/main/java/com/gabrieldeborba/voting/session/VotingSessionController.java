package com.gabrieldeborba.voting.session;

import com.gabrieldeborba.voting.config.ApiVersion;
import com.gabrieldeborba.voting.session.dto.OpenVotingSessionRequest;
import com.gabrieldeborba.voting.session.dto.VotingSessionResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/session")
public class VotingSessionController {

    private static final OpenVotingSessionRequest DEFAULT_REQUEST = new OpenVotingSessionRequest(null);

    private final VotingSessionService service;

    public VotingSessionController(VotingSessionService service) {
        this.service = service;
    }

    /** Opens the session. The body is optional; without it the session lasts 1 minute. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VotingSessionResponse open(
            @PathVariable UUID agendaId, @Valid @RequestBody(required = false) @Nullable OpenVotingSessionRequest request) {
        return service.open(agendaId, request == null ? DEFAULT_REQUEST : request);
    }

    @GetMapping
    public VotingSessionResponse find(@PathVariable UUID agendaId) {
        return service.findByAgendaId(agendaId);
    }
}
