# Assumptions and limitations

1. Fixed seed is `20261022`; generator outputs should be deterministic for the same Java/runtime version.
2. The invoice ID counter is global and IDs embed the zero-padded vendor code.
3. Invoices normally contain one to three lines. The isolated header-total mismatch and invoice-before-PO-date fixtures intentionally contain one line each so each injected condition creates exactly one line-level failure. Header totals remain the values printed on the synthetic invoice.
4. Duplicate detection groups by vendor ID plus invoice number and marks subsequent submissions as duplicates; the first submission is retained as the original.
5. Goods receipt quantity is aggregated across receipt lines for the same PO line before matching.
6. An invoice dated before its PO, an arithmetic mismatch, a closed vendor, a duplicate, a tax mismatch, a missing receipt or a quantity/price variance is not payment-ready.
7. Eligible invoices that pass matching receive two synthetic approvals and a payment-ready record. The scheduled date is fixed for reproducibility; no real payment is initiated.
8. Generated invoice JSON is a synthetic source document for extraction validation; no real vendor PDFs or OCR are used.
9. Invoice headers use normalized JPA persistence. Remaining source/workflow records are saved as typed JSON snapshots and exported to SQL seed tables. A fully normalized JPA mapping is a known extension.
10. `ground_truth` labels are generated separately and are not used to make match/approval/payment decisions.
11. The SQL seed uses text columns to preserve exact generated values portably. A production schema would use typed numeric/date columns and database constraints.

12. One CLEAN-category fixture intentionally has no PO so the nullable invoice PO field and `NO_PURCHASE_ORDER` matching path are exercised. Its expected match status is `NO_PURCHASE_ORDER`, and it is not payment-ready.
