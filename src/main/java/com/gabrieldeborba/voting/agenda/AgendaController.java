package com.gabrieldeborba.voting.agenda;

import com.gabrieldeborba.voting.agenda.dto.AgendaResponse;
import com.gabrieldeborba.voting.agenda.dto.CreateAgendaRequest;
import com.gabrieldeborba.voting.config.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas")
@Tag(name = "Agendas", description = "Topics the cooperative votes on")
public class AgendaController {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final AgendaService service;

    public AgendaController(AgendaService service) {
        this.service = service;
    }

    @Operation(summary = "Register a new agenda")
    @ApiResponse(responseCode = "201", description = "Agenda created; Location header points to it")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed; errors[] lists field and message",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<AgendaResponse> create(@Valid @RequestBody CreateAgendaRequest request) {
        AgendaResponse agenda = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(agenda.id())
                .toUri();
        return ResponseEntity.created(location).body(agenda);
    }

    @Operation(summary = "Get an agenda by id")
    @ApiResponse(responseCode = "200", description = "Agenda found")
    @ApiResponse(
            responseCode = "404",
            description = "Agenda not found",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public AgendaResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Only {@code page} and {@code size} are accepted from the client; sorting is fixed server-side.
     * Out-of-range values are rejected with 400 instead of silently clamped.
     */
    @Operation(summary = "List agendas, newest first", description = "size is capped at 100; page starts at 0")
    @ApiResponse(responseCode = "200", description = "Page of agendas")
    @ApiResponse(
            responseCode = "400",
            description = "page or size out of range; errors[] lists field and message",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PagedModel<AgendaResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return new PagedModel<>(service.findAll(page, size));
    }
}
