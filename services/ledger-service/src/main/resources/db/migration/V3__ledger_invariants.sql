-- Invariants the database enforces on its own, whatever the application does (ADR-0013).

-- I3: entries and transactions are append-only. Corrections are reversing transactions.
CREATE FUNCTION ledger_forbid_change() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ledger table % is append-only: % is not allowed', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER entry_append_only BEFORE UPDATE OR DELETE ON entry
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_change();
CREATE TRIGGER entry_no_truncate BEFORE TRUNCATE ON entry
    FOR EACH STATEMENT EXECUTE FUNCTION ledger_forbid_change();
CREATE TRIGGER transaction_append_only BEFORE UPDATE OR DELETE ON ledger_transaction
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_change();
CREATE TRIGGER transaction_no_truncate BEFORE TRUNCATE ON ledger_transaction
    FOR EACH STATEMENT EXECUTE FUNCTION ledger_forbid_change();

-- I2: at commit time every transaction has at least two entries, and per currency its debits
-- equal its credits. Deferred, so a transaction's entries can be inserted one by one.
CREATE FUNCTION ledger_assert_balanced(tx uuid) RETURNS void
    LANGUAGE plpgsql AS $$
DECLARE
    entry_count integer;
BEGIN
    SELECT count(*) INTO entry_count FROM entry WHERE transaction_id = tx;
    IF entry_count < 2 THEN
        RAISE EXCEPTION 'ledger transaction % has % entries; at least 2 are required', tx, entry_count
            USING ERRCODE = 'check_violation';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM entry
        WHERE transaction_id = tx
        GROUP BY currency
        HAVING sum(CASE side WHEN 'DEBIT' THEN amount ELSE -amount END) <> 0
    ) THEN
        RAISE EXCEPTION 'ledger transaction % does not balance', tx
            USING ERRCODE = 'check_violation';
    END IF;
END;
$$;

CREATE FUNCTION ledger_check_entry_transaction() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    PERFORM ledger_assert_balanced(NEW.transaction_id);
    RETURN NULL;
END;
$$;

CREATE FUNCTION ledger_check_new_transaction() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    PERFORM ledger_assert_balanced(NEW.id);
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER entry_transaction_balanced AFTER INSERT ON entry
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_check_entry_transaction();

CREATE CONSTRAINT TRIGGER transaction_has_balanced_entries AFTER INSERT ON ledger_transaction
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_check_new_transaction();
