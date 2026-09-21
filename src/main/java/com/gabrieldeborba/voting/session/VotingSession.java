package com.gabrieldeborba.voting.session;

import com.gabrieldeborba.voting.agenda.Agenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The voting window of an agenda. Each agenda has at most one session (unique {@code agenda_id}).
 * There is no scheduler closing sessions: a session is open while {@code closesAt} is in the future.
 */
@Entity
@Table(name = "voting_session")
public class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_id", nullable = false, unique = true)
    private Agenda agenda;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    /** Required by JPA. */
    protected VotingSession() {}

    public VotingSession(Agenda agenda, Instant openedAt, Instant closesAt) {
        this.agenda = agenda;
        this.openedAt = openedAt;
        this.closesAt = closesAt;
    }

    public boolean isOpen(Instant now) {
        return closesAt.isAfter(now);
    }

    public VotingSessionStatus status(Instant now) {
        return isOpen(now) ? VotingSessionStatus.OPEN : VotingSessionStatus.CLOSED;
    }

    public @Nullable UUID getId() {
        return id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }
}
