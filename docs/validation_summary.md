# Validation summary template

The live validation report is generated at `data/output/validation_summary.md` when `POST /api/pipeline/run` is executed.

Required checks include configured row counts, primary-key uniqueness, foreign-key integrity, exact scenario proportions, duplicate pair detection, required edge-case tags, one match per invoice line, eligibility for approvals, and the rule that only matched invoices may be payment-ready.

Do not fill in PASS/FAIL results manually. Preserve the actual generated report from the run used for submission.
