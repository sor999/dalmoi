CREATE INDEX ix_gps_events_latest_valid ON gps_events (participant_id, sequence DESC)
    WHERE validation_status = 'VALID';
