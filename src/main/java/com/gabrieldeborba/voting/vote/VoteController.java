package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.config.ApiVersion;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}")
public class VoteController {

    private final VoteService voteService;
    private final VotingResultService resultService;

    public VoteController(VoteService voteService, VotingResultService resultService) {
        this.voteService = voteService;
        this.resultService = resultService;
    }

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable UUID agendaId, @Valid @RequestBody VoteRequest request) {
        return voteService.castVote(agendaId, request);
    }

    /** Available at any time: partial counts while open, final counts plus outcome once closed. */
    @GetMapping("/result")
    public VotingResultResponse result(@PathVariable UUID agendaId) {
        return resultService.result(agendaId);
    }
}
