package com.gabrieldeborba.voting.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gabrieldeborba.voting.agenda.Agenda;
import com.gabrieldeborba.voting.session.dto.OpenVotingSessionRequest;
import com.gabrieldeborba.voting.session.dto.VotingSessionResponse;
import com.gabrieldeborba.voting.session.exception.VotingSessionAlreadyOpenException;
import com.gabrieldeborba.voting.session.exception.VotingSessionNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VotingSessionController.class)
class VotingSessionControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SESSION_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final VotingSessionResponse RESPONSE =
            new VotingSessionResponse(SESSION_ID, AGENDA_ID, NOW, NOW.plusSeconds(60), VotingSessionStatus.OPEN);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VotingSessionService service;

    @Test
    void openWithoutBodyReturns201() throws Exception {
        when(service.open(eq(AGENDA_ID), any())).thenReturn(RESPONSE);

        mockMvc.perform(post("/api/v1/agendas/{id}/session", AGENDA_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.agendaId").value(AGENDA_ID.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.closesAt").value("2026-09-21T12:01:00Z"));

        verify(service).open(AGENDA_ID, new OpenVotingSessionRequest(null));
    }

    @Test
    void openWithDurationPassesItToService() throws Exception {
        when(service.open(eq(AGENDA_ID), any())).thenReturn(RESPONSE);

        mockMvc.perform(post("/api/v1/agendas/{id}/session", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":5}"))
                .andExpect(status().isCreated());

        verify(service).open(AGENDA_ID, new OpenVotingSessionRequest(5));
    }

    @ParameterizedTest(name = "durationMinutes={0} -> 400")
    @ValueSource(ints = {0, -1, 1441})
    void openRejectsDurationOutOfRange(int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{id}/session", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":" + durationMinutes + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("durationMinutes"));

        verify(service, never()).open(any(), any());
    }

    @Test
    void openReturns409WhenSessionAlreadyExists() throws Exception {
        when(service.open(eq(AGENDA_ID), any())).thenThrow(new VotingSessionAlreadyOpenException(AGENDA_ID, new RuntimeException("duplicate key")));

        mockMvc.perform(post("/api/v1/agendas/{id}/session", AGENDA_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Agenda " + AGENDA_ID + " already has a voting session"));
    }

    @Test
    void findReturnsSession() throws Exception {
        when(service.findByAgendaId(AGENDA_ID)).thenReturn(RESPONSE);

        mockMvc.perform(get("/api/v1/agendas/{id}/session", AGENDA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void findReturns404WhenNoSession() throws Exception {
        when(service.findByAgendaId(AGENDA_ID)).thenThrow(new VotingSessionNotFoundException(AGENDA_ID));

        mockMvc.perform(get("/api/v1/agendas/{id}/session", AGENDA_ID))
                .andExpect(status().isNotFound());
    }
}
