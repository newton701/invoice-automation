# Invoice Automation System

## 1. Project Overview

The Invoice Automation System automates invoice processing and validation against purchase orders and goods receipts. It helps identify invoice discrepancies, record matching results, and manage exception cases.

The application exposes REST APIs to generate the dataset, retrieve normalized records, access invoice details, and review matching results.

## 2. Technologies Used

- Java 17
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Postman
- JUnit

## 3. Project Features

- Synthetic procurement dataset generation
- Invoice and purchase order processing
- Invoice matching and validation
- Price and quantity variance detection
- Duplicate invoice detection
- Missing receipt and tax discrepancy detection
- Exception case management
- Approval and payment record retrieval
- REST API integration and testing

## 4. Prerequisites

Install the following before running the application:

- JDK 17
- PostgreSQL
- IntelliJ IDEA or another Java IDE
- Postman

## 5. Database Configuration

Create a PostgreSQL database for the application and configure the connection in `src/main/resources/application.properties`.

Example configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/invoice_automation
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

Use the database name and credentials configured in your local environment. Do not commit real database passwords to GitHub.

## 6. Run the Application

Clone the repository:

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
cd invoice-automation
```

Run the tests:

```bash
.\mvnw.cmd test
```

Start the Spring Boot application:

```bash
.\mvnw.cmd spring-boot:run
```

The application runs on:

`http://localhost:8080`

## 7. REST API Documentation

All endpoints below use the base URL:

`http://localhost:8080`

### 7.1 Pipeline APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/pipeline/run` | Runs the invoice processing pipeline |
| GET | `/api/pipeline/health` | Checks pipeline health |

Example pipeline request:

```http
POST http://localhost:8080/api/pipeline/run
```

No request body is required by the controller mapping currently documented.

### 7.2 Dataset APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/datasets/counts` | Retrieves dataset record counts |
| GET | `/api/datasets/{recordType}` | Retrieves records for a specified record type |

Example:

```http
GET http://localhost:8080/api/datasets/counts
```

### 7.3 Invoice APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/invoices` | Retrieves invoices |
| GET | `/api/invoices/count` | Retrieves the invoice count |
| GET | `/api/invoices/{invoiceId}` | Retrieves an invoice by ID |
| GET | `/api/invoices/vendor/{vendorId}` | Retrieves invoices for a vendor |
| GET | `/api/invoices/match-status/{status}` | Retrieves invoices by matching status |

Examples:

```http
GET http://localhost:8080/api/invoices
GET http://localhost:8080/api/invoices/count
GET http://localhost:8080/api/invoices/INV-00160-2026-000002
GET http://localhost:8080/api/invoices/vendor/VND-00160
GET http://localhost:8080/api/invoices/match-status/MATCHED
```

### 7.4 Normalized Data APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/vendors` | Retrieves vendors |
| GET | `/api/purchase-orders` | Retrieves purchase orders |
| GET | `/api/purchase-order-lines` | Retrieves purchase order lines |
| GET | `/api/goods-receipts` | Retrieves goods receipts |
| GET | `/api/goods-receipt-lines` | Retrieves goods receipt lines |
| GET | `/api/invoice-lines` | Retrieves invoice lines |
| GET | `/api/ground-truth` | Retrieves ground-truth records |
| GET | `/api/match-results` | Retrieves invoice matching results |
| GET | `/api/exception-cases` | Retrieves invoice exceptions |
| GET | `/api/approvals` | Retrieves approval records |
| GET | `/api/payments` | Retrieves payment records |
| GET | `/api/invoice-status-events` | Retrieves invoice status history |
| GET | `/api/normalized-counts` | Retrieves normalized record counts |

Example:

```http
GET http://localhost:8080/api/vendors
GET http://localhost:8080/api/purchase-orders
GET http://localhost:8080/api/match-results
GET http://localhost:8080/api/exception-cases
GET http://localhost:8080/api/normalized-counts
```

## 8. Testing with Postman

1. Start the Spring Boot application.
2. Open Postman.
3. Create a request and select the appropriate HTTP method.
4. Enter the endpoint using `http://localhost:8080`.
5. Click **Send**.
6. Review the HTTP status code and JSON response.

Use `POST` for running the pipeline and `GET` for retrieving records.

## 9. Error Handling

The application uses HTTP status codes to indicate request outcomes. A `500 Internal Server Error` indicates that the request encountered a server-side problem. Check the Spring Boot console logs and PostgreSQL constraints to identify the cause.

## 10. Testing

Run automated tests using:

```bash
.\mvnw.cmd test
```

Ensure all tests pass before pushing changes to GitHub.

## 11. Future Improvements

- Add API documentation using Swagger/OpenAPI.
- Add pagination and filtering where needed.
- Improve exception reporting and validation diagnostics.
- Add integration tests for PostgreSQL.
- Document dataset generation and output files in detail.

## 12. Security Notes

- Never commit database passwords, API keys, or other secrets.
- Keep local configuration separate from public configuration.
- Verify generated data and pipeline validation results before publishing the repository.
