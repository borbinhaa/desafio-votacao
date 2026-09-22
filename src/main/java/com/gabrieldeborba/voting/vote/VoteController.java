package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.config.ApiVersion;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/votes")
public class VoteController {

    private final VoteService service;

    public VoteController(VoteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable UUID agendaId, @Valid @RequestBody VoteRequest request) {
        return service.castVote(agendaId, request);
    }
}
