package com.gabrieldeborba.voting.session;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingSessionRepository extends JpaRepository<VotingSession, UUID> {

    Optional<VotingSession> findByAgendaId(UUID agendaId);
}
