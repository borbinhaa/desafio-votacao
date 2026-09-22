package com.gabrieldeborba.voting.session;

import com.gabrieldeborba.voting.config.ApiVersion;
import com.gabrieldeborba.voting.session.dto.OpenVotingSessionRequest;
import com.gabrieldeborba.voting.session.dto.VotingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/session")
@Tag(name = "Voting sessions", description = "The time window in which an agenda accepts votes")
public class VotingSessionController {

    private static final OpenVotingSessionRequest DEFAULT_REQUEST = new OpenVotingSessionRequest(null);

    private final VotingSessionService service;

    public VotingSessionController(VotingSessionService service) {
        this.service = service;
    }

    /** Opens the session. The body is optional; without it the session lasts 1 minute. */
    @Operation(
            summary = "Open the voting session of an agenda",
            description = "Body is optional: without durationMinutes the session stays open for 1 minute. "
                    + "Each agenda has exactly one session; it closes by itself when closesAt is reached.")
    @ApiResponse(responseCode = "201", description = "Session opened")
    @ApiResponse(responseCode = "400", description = "durationMinutes out of range (1..1440); errors[] lists field and message", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Agenda already has a session", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VotingSessionResponse open(
            @PathVariable UUID agendaId, @Valid @RequestBody(required = false) @Nullable OpenVotingSessionRequest request) {
        return service.open(agendaId, request == null ? DEFAULT_REQUEST : request);
    }

    @Operation(summary = "Get the session of an agenda", description = "status is OPEN or CLOSED, derived from closesAt")
    @ApiResponse(responseCode = "200", description = "Session found")
    @ApiResponse(responseCode = "404", description = "Agenda not found, or agenda has no session", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public VotingSessionResponse find(@PathVariable UUID agendaId) {
        return service.findByAgendaId(agendaId);
    }
}
