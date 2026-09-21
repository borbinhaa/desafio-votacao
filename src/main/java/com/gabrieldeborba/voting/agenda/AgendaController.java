package com.gabrieldeborba.voting.agenda;

import com.gabrieldeborba.voting.config.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.web.PagedModel;
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
public class AgendaController {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final AgendaService service;

    public AgendaController(AgendaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AgendaResponse> create(@Valid @RequestBody CreateAgendaRequest request) {
        AgendaResponse agenda = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(agenda.id())
                .toUri();
        return ResponseEntity.created(location).body(agenda);
    }

    @GetMapping("/{id}")
    public AgendaResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Only {@code page} and {@code size} are accepted from the client; sorting is fixed server-side.
     * Out-of-range values are rejected with 400 instead of silently clamped.
     */
    @GetMapping
    public PagedModel<AgendaResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return new PagedModel<>(service.findAll(page, size));
    }
}
