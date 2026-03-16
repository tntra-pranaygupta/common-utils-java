-- ============================================================
-- V2__create_transaction_table.sql
-- Payments domain: core transaction ledger table.
--
-- PCI/DSS notes:
--   • Raw PAN values are NEVER stored. Only the last 4 digits
--     (pan_last_four) and a tokenised reference (pan_token) are
--     persisted. The token is resolved by the Token Vault service.
--   • Amount is stored as NUMERIC(19,4) to avoid floating-point
--     rounding errors — a hard requirement for financial data.
--   • sensitive columns (pan_token) are tagged with a comment so
--     that DBA tooling can enforce column-level encryption policies.
--   • All timestamps use TIMESTAMP WITH TIME ZONE.
-- ============================================================

-- ─── 1. transaction_status ENUM ───────────────────────────────────────────────
-- Using a CHECK constraint instead of a DB-specific ENUM type keeps the
-- migration portable across PostgreSQL and H2 (used in tests).
-- To add a new status: add a new migration that alters the constraint.

CREATE TABLE IF NOT EXISTS financial_transaction
(
    -- ── Identity ──────────────────────────────────────────────────────────────
    id                  BIGSERIAL                   NOT NULL,
    -- UUID exposed externally; internal BIGSERIAL is never leaked to clients.
    external_id         VARCHAR(36)                 NOT NULL,   -- UUID v4

    -- ── Payment instrument (PCI/DSS: no raw PAN) ──────────────────────────────
    -- Token issued by Token Vault; resolved at charge time; never logged.
    pan_token           VARCHAR(64),                            -- PCI: SENSITIVE — encrypt at rest
    pan_last_four       CHAR(4),                                -- safe to display in UI
    card_scheme         VARCHAR(32),                            -- VISA | MASTERCARD | AMEX | ...

    -- ── Financials ────────────────────────────────────────────────────────────
    amount              NUMERIC(19, 4)              NOT NULL,
    currency            CHAR(3)                     NOT NULL,   -- ISO-4217, e.g. 'GBP'

    -- ── Lifecycle ─────────────────────────────────────────────────────────────
    status              VARCHAR(32)                 NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(512),                           -- populated on FAILED / DECLINED

    -- ── Audit fields (mirror AuditableEntity) ─────────────────────────────────
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),

    -- ── Observability ─────────────────────────────────────────────────────────
    correlation_id      VARCHAR(64),                            -- from X-Correlation-Id header

    -- ── Constraints ───────────────────────────────────────────────────────────
    CONSTRAINT pk_financial_transaction         PRIMARY KEY (id),
    CONSTRAINT uq_financial_transaction_ext_id  UNIQUE  (external_id),
    CONSTRAINT chk_transaction_status           CHECK   (status IN (
        'PENDING', 'AUTHORISED', 'CAPTURED', 'SETTLED',
        'FAILED', 'DECLINED', 'REFUNDED', 'CANCELLED'
    )),
    CONSTRAINT chk_transaction_amount_positive  CHECK   (amount > 0),
    CONSTRAINT chk_pan_last_four_numeric        CHECK   (pan_last_four ~ '^[0-9]{4}$' OR pan_last_four IS NULL)
);

-- ─── Indexes ───────────────────────────────────────────────────────────────────

-- Primary look-up pattern: external consumers query by external_id
CREATE INDEX IF NOT EXISTS idx_ft_external_id
    ON financial_transaction (external_id);

-- Support for status-based dashboards and batch settlement jobs
CREATE INDEX IF NOT EXISTS idx_ft_status
    ON financial_transaction (status);

-- Time-series queries (e.g., reporting, reconciliation)
CREATE INDEX IF NOT EXISTS idx_ft_created_at
    ON financial_transaction (created_at DESC);

-- Correlation look-up (incident investigation / distributed tracing)
CREATE INDEX IF NOT EXISTS idx_ft_correlation_id
    ON financial_transaction (correlation_id);

-- ─── Column security comment (informational for DBA tooling) ──────────────────
COMMENT ON COLUMN financial_transaction.pan_token IS
    'PCI/DSS SENSITIVE: must be encrypted at rest via TDE or column-level encryption. '
    'Resolved only at charge time via Token Vault. Must never appear in application logs.';
