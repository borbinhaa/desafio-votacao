package com.gabrieldeborba.voting.vote;

import com.gabrieldeborba.voting.agenda.Agenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One member's vote on an agenda. The pair (agenda, member CPF) is unique at the database level, which
 * is what guarantees "one vote per member per agenda" even under concurrent requests.
 */
@Entity
@Table(
        name = "vote",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_vote_agenda_member",
                        columnNames = {"agenda_id", "member_cpf"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_id", nullable = false)
    private Agenda agenda;

    @Column(name = "member_cpf", nullable = false, length = 11)
    private String memberCpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private VoteChoice choice;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    /** Required by JPA. */
    protected Vote() {}

    public Vote(Agenda agenda, String memberCpf, VoteChoice choice, Instant votedAt) {
        this.agenda = agenda;
        this.memberCpf = memberCpf;
        this.choice = choice;
        this.votedAt = votedAt;
    }

    public @Nullable UUID getId() {
        return id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public String getMemberCpf() {
        return memberCpf;
    }

    public VoteChoice getChoice() {
        return choice;
    }

    public Instant getVotedAt() {
        return votedAt;
    }
}
