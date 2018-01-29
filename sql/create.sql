BEGIN;
  CREATE TABLE npc_kill_data(
    id SERIAL,
    system_id BIGINT NOT NULL,
    npc_kills INT NOT NULL,
    from_tstamp timestamptz NOT NULL,
    to_tstamp timestamptz NOT NULL,
    source TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (system_id, from_tstamp, to_tstamp, source)
  );
COMMIT;