package com.gabrieldeborba.voting.agenda;

import com.gabrieldeborba.voting.agenda.dto.AgendaResponse;
import com.gabrieldeborba.voting.agenda.dto.CreateAgendaRequest;
import com.gabrieldeborba.voting.agenda.exception.AgendaNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    private final AgendaRepository repository;
    private final Clock clock;

    public AgendaService(AgendaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public AgendaResponse create(CreateAgendaRequest request) {
        Agenda agenda = repository.save(new Agenda(request.title(), request.description(), clock.instant()));
        log.info("Agenda created: id={} title=\"{}\"", agenda.getId(), agenda.getTitle());
        return AgendaResponse.from(agenda);
    }

    @Transactional(readOnly = true)
    public AgendaResponse findById(UUID id) {
        return AgendaResponse.from(getAgenda(id));
    }

    /** Lists agendas newest first. Paging bounds are validated by the controller. */
    @Transactional(readOnly = true)
    public Page<AgendaResponse> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(pageRequest).map(AgendaResponse::from);
    }

    /** Loads the entity for other services (session, vote); throws 404 when missing. */
    @Transactional(readOnly = true)
    public Agenda getAgenda(UUID id) {
        return repository.findById(id).orElseThrow(() -> new AgendaNotFoundException(id));
    }
}
