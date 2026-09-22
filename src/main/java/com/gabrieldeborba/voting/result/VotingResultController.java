package com.gabrieldeborba.voting.result;

import com.gabrieldeborba.voting.config.ApiVersion;
import com.gabrieldeborba.voting.result.dto.VotingResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/result")
@Tag(name = "Results", description = "Vote tally of an agenda")
public class VotingResultController {

    private final VotingResultService service;

    public VotingResultController(VotingResultService service) {
        this.service = service;
    }

    /** Available at any time: partial counts while open, final counts plus outcome once closed. */
    @Operation(
            summary = "Get the voting result of an agenda",
            description = "Counts are always returned. outcome (APPROVED, REJECTED, TIED) is present only "
                    + "when status is CLOSED; status is NOT_OPENED, OPEN or CLOSED.")
    @ApiResponse(responseCode = "200", description = "Tally")
    @ApiResponse(responseCode = "404", description = "Agenda not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public VotingResultResponse result(@PathVariable UUID agendaId) {
        return service.result(agendaId);
    }
}
