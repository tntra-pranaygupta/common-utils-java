-- ============================================================
-- V1__init_schema.sql
-- Platform bootstrap: create the audit infrastructure tables
-- that the AuditableEntity base class relies on.
--
-- PCI/DSS notes:
--   • No PAN, PII, or card-holder data is stored in this schema.
--   • All tables use TIMESTAMP WITH TIME ZONE (timestamptz) to
--     avoid ambiguous local-time audit trails across data centres.
--   • Table and column names match the field mappings in
--     AuditableEntity.java (@Column annotations).
-- ============================================================

-- ─── Schema version metadata ──────────────────────────────────────────────────
-- Tracked by Flyway in the `flyway_schema_history` table (auto-created).

-- ─── 1. audit_log ─────────────────────────────────────────────────────────────
-- Generic, immutable audit trail. Services write a row whenever a state
-- transition occurs on a sensitive aggregate root.
CREATE TABLE IF NOT EXISTS audit_log
(
    id              BIGSERIAL                   NOT NULL,
    entity_type     VARCHAR(128)                NOT NULL,   -- e.g. 'Payment', 'Account'
    entity_id       VARCHAR(64)                 NOT NULL,   -- opaque external ID
    action          VARCHAR(64)                 NOT NULL,   -- CREATE | UPDATE | DELETE
    performed_by    VARCHAR(255),                           -- username / service principal
    performed_at    TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    change_summary  TEXT,                                   -- human-readable diff (no PAN!)
    correlation_id  VARCHAR(64),                            -- ties to HTTP X-Correlation-Id

    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

-- Index to support look-ups by entity (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_audit_log_entity
    ON audit_log (entity_type, entity_id);

-- Index to support look-ups by who performed an action (GDPR access requests)
CREATE INDEX IF NOT EXISTS idx_audit_log_performed_by
    ON audit_log (performed_by);
