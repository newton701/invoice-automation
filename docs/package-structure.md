# Package Structure

```text
com.invoiceautomation
├── controller/   # REST API controllers
├── service/      # Pipeline/business logic
├── repository/   # Spring Data JPA repositories
├── entity/       # JPA database entities and converters
├── model/        # Domain/data models
├── generator/    # Dataset and invoice generation
├── matching/     # Matching logic
├── validation/   # Validation logic
├── workflow/     # Approval workflow
├── payment/      # Payment processing
├── extraction/   # Document extraction
├── export/       # CSV/JSON export
├── config/       # Configuration
├── enums/        # Enums
└── util/         # Utilities
```

All REST controllers are in `controller`, all JPA repositories are in `repository`, and all JPA entities are in `entity`.
