-- Lets "count(*) ... where agenda_id = ? group by choice" (the result endpoint) run as an
-- index-only scan instead of reading every vote row of the agenda.
CREATE INDEX idx_vote_agenda_choice ON vote (agenda_id, choice);
