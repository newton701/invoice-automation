# Normalized PostgreSQL persistence

The pipeline persists generated data to the following normalized tables:

| Table | Entity | Repository |
|---|---|---|
| `vendors` | `VendorEntity` | `VendorRepository` |
| `purchase_orders` | `PurchaseOrderEntity` | `PurchaseOrderRepository` |
| `purchase_order_lines` | `PurchaseOrderLineEntity` | `PurchaseOrderLineRepository` |
| `goods_receipts` | `GoodsReceiptEntity` | `GoodsReceiptRepository` |
| `goods_receipt_lines` | `GoodsReceiptLineEntity` | `GoodsReceiptLineRepository` |
| `invoices` | `InvoiceEntity` | `InvoiceRepository` |
| `invoice_lines` | `InvoiceLineEntity` | `InvoiceLineRepository` |
| `ground_truth` | `GroundTruthEntity` | `GroundTruthRepository` |
| `match_results` | `MatchResultEntity` | `MatchResultRepository` |
| `exception_cases` | `ExceptionCaseEntity` | `ExceptionCaseRepository` |
| `approvals` | `ApprovalEntity` | `ApprovalRepository` |
| `payments` | `PaymentEntity` | `PaymentRepository` |
| `invoice_status_events` | `InvoiceStatusEventEntity` | `InvoiceStatusEventRepository` |

The existing `generated_records` table is retained as a JSON snapshot table; it has not been removed.

## Read endpoints

- `GET /api/vendors`
- `GET /api/purchase-orders`
- `GET /api/purchase-order-lines`
- `GET /api/goods-receipts`
- `GET /api/goods-receipt-lines`
- `GET /api/invoices` (existing endpoint)
- `GET /api/invoice-lines`
- `GET /api/ground-truth`
- `GET /api/match-results`
- `GET /api/exception-cases`
- `GET /api/approvals`
- `GET /api/payments`
- `GET /api/invoice-status-events`
- `GET /api/normalized-counts`

The normalized GET endpoints query Spring Data repositories, which read the PostgreSQL tables. Run the pipeline with `POST /api/pipeline/run` to regenerate and persist the dataset.
