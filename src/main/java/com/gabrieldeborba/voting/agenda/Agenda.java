package com.gabrieldeborba.voting.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A topic that members of the cooperative vote on. */
@Entity
@Table(name = "agenda")
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private @Nullable String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Agenda() {}

    public Agenda(String title, @Nullable String description, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    public @Nullable UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
