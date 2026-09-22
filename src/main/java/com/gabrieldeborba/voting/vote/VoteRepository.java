package com.gabrieldeborba.voting.vote;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    /** Aggregates in the database so the result costs one query regardless of the number of votes. */
    @Query("select new com.gabrieldeborba.voting.vote.VoteCount(v.choice, count(v)) "
            + "from Vote v where v.agenda.id = :agendaId group by v.choice")
    List<VoteCount> countByAgendaIdGroupedByChoice(UUID agendaId);
}
