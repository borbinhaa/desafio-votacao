package com.gabrieldeborba.voting.agenda;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaRepository extends JpaRepository<Agenda, UUID> {}
