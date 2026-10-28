-- Compatibility migration: the earlier prototype added a uniqueness constraint that conflicts with
-- the required labelled duplicate-invoice-number scenario. Safe when the table/constraint is absent.
ALTER TABLE IF EXISTS invoices DROP CONSTRAINT IF EXISTS uk_invoice_vendor_number;
