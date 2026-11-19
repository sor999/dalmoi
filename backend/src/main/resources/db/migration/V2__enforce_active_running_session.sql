DO $$
BEGIN
    IF EXISTS (
        SELECT crew_id FROM running_sessions
        WHERE status IN ('WAITING', 'RUNNING')
        GROUP BY crew_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Multiple active sessions exist for a crew; resolve duplicates before migration';
    END IF;
END $$;

DROP INDEX uq_running_session_per_crew;
CREATE UNIQUE INDEX uq_running_session_per_crew ON running_sessions (crew_id)
    WHERE status IN ('WAITING', 'RUNNING');
