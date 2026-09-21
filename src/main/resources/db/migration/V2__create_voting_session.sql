CREATE TABLE voting_session (
    id         UUID        PRIMARY KEY,
    agenda_id  UUID        NOT NULL UNIQUE REFERENCES agenda (id),
    opened_at  TIMESTAMPTZ NOT NULL,
    closes_at  TIMESTAMPTZ NOT NULL
);
