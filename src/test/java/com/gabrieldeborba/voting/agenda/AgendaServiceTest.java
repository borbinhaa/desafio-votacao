package com.gabrieldeborba.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private AgendaRepository repository;

    private AgendaService service;

    @BeforeEach
    void setUp() {
        service = new AgendaService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createStampsCreationTimeFromClock() {
        when(repository.save(any(Agenda.class))).thenAnswer(invocation -> {
            Agenda agenda = invocation.getArgument(0);
            ReflectionTestUtils.setField(agenda, "id", ID);
            return agenda;
        });

        AgendaResponse response = service.create(new CreateAgendaRequest("Budget 2026", "Annual budget"));

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("Budget 2026");
        assertThat(response.description()).isEqualTo("Annual budget");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void findByIdReturnsAgenda() {
        when(repository.findById(ID)).thenReturn(Optional.of(new Agenda("Title", null, NOW)));

        AgendaResponse response = service.findById(ID);

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.description()).isNull();
    }

    @Test
    void findAllPagesNewestFirst() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.findAll(2, 50);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void findByIdThrowsWhenAgendaDoesNotExist() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID))
                .isInstanceOf(AgendaNotFoundException.class)
                .hasMessage("Agenda " + ID + " not found");
    }
}
