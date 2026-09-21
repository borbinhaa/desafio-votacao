package com.gabrieldeborba.voting.agenda;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgendaController.class)
class AgendaControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendaService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(new AgendaResponse(ID, "Budget 2026", "Annual budget", NOW));

        mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Budget 2026\",\"description\":\"Annual budget\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/agendas/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.title").value("Budget 2026"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-21T12:00:00Z"));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(service.findById(ID)).thenThrow(new AgendaNotFoundException(ID));

        mockMvc.perform(get("/api/v1/agendas/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Agenda " + ID + " not found"));
    }

    @Test
    void findByIdRejectsMalformedUuid() throws Exception {
        mockMvc.perform(get("/api/v1/agendas/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void findAllReturnsPagedContent() throws Exception {
        when(service.findAll(0, 20))
                .thenReturn(new PageImpl<>(List.of(new AgendaResponse(ID, "Budget 2026", null, NOW))));

        mockMvc.perform(get("/api/v1/agendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Budget 2026"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void findAllPassesExplicitPageAndSize() throws Exception {
        when(service.findAll(2, 100)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/agendas").param("page", "2").param("size", "100"))
                .andExpect(status().isOk());

        verify(service).findAll(2, 100);
    }

    @ParameterizedTest(name = "page={0} size={1} -> 400 on {2}")
    @CsvSource({
        "0, 1000000, size", // above the cap
        "0, 0, size", // must be at least 1
        "0, -5, size",
        "-1, 10, page", // negative page
        "abc, 10, page" // not a number
    })
    void findAllRejectsInvalidPagingParameters(String page, String size, String field) throws Exception {
        mockMvc.perform(get("/api/v1/agendas").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(service, never()).findAll(anyInt(), anyInt());
    }
}
