package com.gabrieldeborba.voting.result;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gabrieldeborba.voting.agenda.exception.AgendaNotFoundException;
import com.gabrieldeborba.voting.result.dto.VotingResultResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VotingResultController.class)
class VotingResultControllerTest {

    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VotingResultService resultService;

    @Test
    void resultOmitsOutcomeWhileSessionIsOpen() throws Exception {
        when(resultService.result(AGENDA_ID))
                .thenReturn(new VotingResultResponse(AGENDA_ID, "Budget 2026", VotingStatus.OPEN, 3, 1, 4, null));

        mockMvc.perform(get("/api/v1/agendas/{id}/result", AGENDA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendaId").value(AGENDA_ID.toString()))
                .andExpect(jsonPath("$.title").value("Budget 2026"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.yesVotes").value(3))
                .andExpect(jsonPath("$.noVotes").value(1))
                .andExpect(jsonPath("$.totalVotes").value(4))
                .andExpect(jsonPath("$.outcome").doesNotExist());
    }

    @Test
    void resultIncludesOutcomeOnceClosed() throws Exception {
        when(resultService.result(AGENDA_ID))
                .thenReturn(new VotingResultResponse(
                        AGENDA_ID, "Budget 2026", VotingStatus.CLOSED, 3, 1, 4, VotingOutcome.APPROVED));

        mockMvc.perform(get("/api/v1/agendas/{id}/result", AGENDA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.outcome").value("APPROVED"));
    }

    @Test
    void resultReturns404WhenAgendaDoesNotExist() throws Exception {
        when(resultService.result(AGENDA_ID)).thenThrow(new AgendaNotFoundException(AGENDA_ID));

        mockMvc.perform(get("/api/v1/agendas/{id}/result", AGENDA_ID)).andExpect(status().isNotFound());
    }
}
