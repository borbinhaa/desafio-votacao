CREATE TABLE agenda (
    id          UUID          PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    created_at  TIMESTAMPTZ   NOT NULL
);
