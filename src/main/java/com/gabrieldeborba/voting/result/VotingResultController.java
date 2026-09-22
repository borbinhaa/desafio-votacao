package com.gabrieldeborba.voting.result;

import com.gabrieldeborba.voting.config.ApiVersion;
import com.gabrieldeborba.voting.result.dto.VotingResultResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/agendas/{agendaId}/result")
public class VotingResultController {

    private final VotingResultService service;

    public VotingResultController(VotingResultService service) {
        this.service = service;
    }

    /** Available at any time: partial counts while open, final counts plus outcome once closed. */
    @GetMapping
    public VotingResultResponse result(@PathVariable UUID agendaId) {
        return service.result(agendaId);
    }
}
