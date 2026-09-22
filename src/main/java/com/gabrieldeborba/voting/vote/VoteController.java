package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.config.ApiVersion;
import com.gabrieldeborba.voting.vote.dto.VoteRequest;
import com.gabrieldeborba.voting.vote.dto.VoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/votes")
@Tag(name = "Votes", description = "One YES/NO vote per member (CPF) per agenda")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(
            summary = "Cast a vote",
            description = "The member is identified by CPF. The external CPF service (fake in this project) "
                    + "decides at random whether the member may vote.")
    @ApiResponse(responseCode = "201", description = "Vote registered (the CPF is not echoed back)")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed (CPF format, choice); errors[] lists field and message",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Agenda not found, or CPF invalid",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Member already voted on this agenda",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Session not open / closed, or member unable to vote",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable UUID agendaId, @Valid @RequestBody VoteRequest request) {
        return voteService.castVote(agendaId, request);
    }
}
