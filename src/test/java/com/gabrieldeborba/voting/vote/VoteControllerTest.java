package com.gabrieldeborba.voting.vote;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VoteController.class)
class VoteControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID AGENDA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID VOTE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoteService service;

    @MockitoBean
    private VotingResultService resultService;

    @Test
    void castVoteReturns201WithoutEchoingCpf() throws Exception {
        when(service.castVote(eq(AGENDA_ID), any()))
                .thenReturn(new VoteResponse(VOTE_ID, AGENDA_ID, VoteChoice.YES, NOW));

        mockMvc.perform(post("/api/v1/agendas/{id}/votes", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(VOTE_ID.toString()))
                .andExpect(jsonPath("$.agendaId").value(AGENDA_ID.toString()))
                .andExpect(jsonPath("$.choice").value("YES"))
                .andExpect(jsonPath("$.votedAt").value("2026-09-21T12:00:00Z"))
                .andExpect(jsonPath("$.cpf").doesNotExist());

        verify(service).castVote(AGENDA_ID, new VoteRequest("12345678909", VoteChoice.YES));
    }

    @ParameterizedTest(name = "body {0} -> 400 on {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"cpf\":\"123\",\"choice\":\"YES\"}          | cpf",
                "{\"cpf\":\"123.456.789-09\",\"choice\":\"NO\"} | cpf",
                "{\"choice\":\"YES\"}                            | cpf",
                "{\"cpf\":\"12345678909\"}                       | choice"
            })
    void castVoteRejectsInvalidFields(String body, String field) throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{id}/votes", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verify(service, never()).castVote(any(), any());
    }

    @Test
    void castVoteRejectsUnknownChoice() throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{id}/votes", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\",\"choice\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));

        verify(service, never()).castVote(any(), any());
    }

    @Test
    void castVoteReturns409WhenMemberAlreadyVoted() throws Exception {
        when(service.castVote(eq(AGENDA_ID), any()))
                .thenThrow(new MemberAlreadyVotedException(AGENDA_ID));

        mockMvc.perform(post("/api/v1/agendas/{id}/votes", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\",\"choice\":\"YES\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Member has already voted on agenda " + AGENDA_ID));
    }

    @Test
    void castVoteReturns422WhenSessionIsClosed() throws Exception {
        when(service.castVote(eq(AGENDA_ID), any())).thenThrow(new VotingSessionClosedException(AGENDA_ID, NOW));

        mockMvc.perform(post("/api/v1/agendas/{id}/votes", AGENDA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\",\"choice\":\"NO\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Voting session for agenda " + AGENDA_ID + " closed at " + NOW));
    }

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
}
