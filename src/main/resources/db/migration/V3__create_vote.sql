CREATE TABLE vote (
    id         UUID        PRIMARY KEY,
    agenda_id  UUID        NOT NULL REFERENCES agenda (id),
    member_cpf VARCHAR(11) NOT NULL,
    choice     VARCHAR(3)  NOT NULL CHECK (choice IN ('YES', 'NO')),
    voted_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_vote_agenda_member UNIQUE (agenda_id, member_cpf)
);
