
# AI HANDOFF AND SYSTEM CONTEXT DOCUMENT

> **Purpose:** This document provides dense, comprehensive, and verified architectural and code-level context so that another AI assistant can instantly understand this codebase, debug issues, develop new features, or assist the user in technical interviews without re-analyzing the repository.

---

## 1. Project Identity & Verified Technology Stack

- **Project Name:** Transaction Monitoring System
- **Repository Root:** `c:\Users\NEHA\OneDrive\Desktop\Study\transaction-monitoring-system`
- **Primary Domain:** Financial Fraud Detection, Anti-Money Laundering (AML), Batch Ingestion, Real-Time Microservice Inference.
- **Backend Stack:**
  - Java 21 (LTS)
  - Spring Boot 4.1.0-SNAPSHOT (Parent: `spring-boot-starter-parent:4.1.0`)
  - Spring Web MVC, Spring Data JPA, Hibernate ORM
  - Jakarta Validation (Bean Validation with `@Min`, `@PositiveOrZero`)
  - Project Lombok (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`)
  - OpenCSV 5.10 (Streaming CSV parsing)
  - PostgreSQL Driver (`org.postgresql:postgresql`)
  - Spring 6.1+ `RestClient` (Synchronous HTTP client)
- **Machine Learning Microservice Stack:**
  - Python 3.13 (Virtual environment in `ml-service/venv`)
  - FastAPI 0.115+, Uvicorn 0.34+ (ASGI HTTP service)
  - Scikit-learn 1.6+, Pandas 2.2+, NumPy 2.2+, Joblib 1.4+
  - Serialized Models: `fraud_model.pkl` (`LogisticRegression`), `scaler.pkl` (`StandardScaler`), `threshold.pkl` (Float `0.95`).
- **Database:**
  - PostgreSQL running locally on port 5432 (`transaction_monitoring`).
  - DDL strategy: Hibernate auto-ddl `update` (`spring.jpa.hibernate.ddl-auto=update`).
- **Infrastructure & Frontend Status:**
  - `docker-compose.yml`: Present in root, but currently empty (0 bytes).
  - `frontend/`: Directory committed in git, but currently empty (no HTML/CSS/JS files).

---

## 2. Directory & Source Code Structure

```
transaction-monitoring-system/
├── backend/
│   ├── pom.xml                                   # Maven dependencies and build configuration
│   ├── src/main/java/com/nehakedar/backend/
│   │   ├── BackendApplication.java               # Spring Boot entry point
│   │   ├── config/RestClientConfig.java          # RestClient singleton bean
│   │   ├── constant/                             # Enums: ModelVersion, ProcessingStatus, Severity
│   │   ├── controller/
│   │   │   ├── TransactionController.java        # /api/transactions (GET, search, {id})
│   │   │   ├── DashboardController.java          # /api/dashboard (summary, amount, recent, status)
│   │   │   ├── FraudDetectionController.java     # /api/fraud/predict (POST)
│   │   │   └── DatasetImportController.java      # /api/import (POST)
│   │   ├── dto/                                  # Request/response DTOs
│   │   ├── entity/
│   │   │   ├── Transaction.java                  # Main transaction entity
│   │   │   ├── TransactionFeatures.java          # PCA features V1..V28
│   │   │   └── Prediction.java                   # ML prediction result entity
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java       # @ControllerAdvice HTTP error response mapper
│   │   │   └── TransactionNotFoundException.java # RuntimeException for missing IDs
│   │   ├── repository/
│   │   │   ├── TransactionRepository.java        # Queries, JPA Specification executor
│   │   │   ├── TransactionFeaturesRepository.java# Repository for V1..V28 features
│   │   │   └── PredictionRepository.java         # Repository for Prediction entity
│   │   ├── service/
│   │   │   ├── TransactionService.java           # Pagination, lookup, specification filtering
│   │   │   ├── DashboardService.java             # Dashboard metrics and aggregations
│   │   │   ├── FraudDetectionService.java        # RestClient wrapper to ML service
│   │   │   └── DatasetImportService.java         # OpenCSV batch ingestion service
│   │   └── specifications/
│   │       └── TransactionSpecification.java     # JPA Criteria API predicate builders
│   └── src/main/resources/
│       ├── application.properties                # Spring, DB, port, and ML URL config
│       └── dataset/creditcard.csv                # 284,807-row Kaggle dataset (~147 MB)
├── ml-service/
│   ├── requirements.txt                          # Python dependencies
│   ├── app/
│   │   ├── main.py                               # FastAPI application routes (GET /, POST /predict)
│   │   ├── predict.py                            # Feature engineering, scaling, and inference
│   │   ├── train_model.py                        # Standalone IsolationForest test script
│   │   └── eda.py                                # Exploratory data analysis script
│   ├── model/
│   │   ├── fraud_model.pkl                       # Trained Logistic Regression model
│   │   ├── scaler.pkl                            # Fitted StandardScaler
│   │   └── threshold.pkl                         # Selected threshold (0.95)
│   └── notebooks/
│       └── preprocessing.ipynb                   # End-to-end data cleaning, EDA, & training
├── project-audit-report/                         # Comprehensive technical audit reports
└── docker-compose.yml                            # Empty (0-byte) deployment file
```

---

## 3. Verified End-to-End Execution Flows

### Flow 1: ML Inference Request
1. Client sends `POST /api/fraud/predict` with JSON body containing `Time`, `V1`..`V28`, `Amount`.
2. `FraudDetectionController.predict()` calls `fraudDetectionService.predict(request)`.
3. `FraudDetectionService` sends HTTP POST via Spring's `RestClient` to `http://127.0.0.1:8000/predict`.
4. FastAPI `main.py` validates input using Pydantic `TransactionRequest` and passes data to `predict_transaction()`.
5. `predict.py` executes:
   - `amount_log = np.log1p(amount)`
   - `time_of_day = time % 86400; hour = time_of_day / 3600; hour_angle = 2 * np.pi * hour / 24`
   - `time_sin = np.sin(hour_angle); time_cos = np.cos(hour_angle)`
   - Builds a 31-feature vector `[V1..V28, amount_log, time_sin, time_cos]`.
   - Normalizes vector using `scaler.transform(features)`.
   - Calculates fraud probability: `prob = model.predict_proba(scaled)[0][1]`.
   - Compares: `is_fraud = prob >= 0.95`.
6. Returns `{"fraud": bool, "probability": float, "threshold": 0.95}` through Spring Boot to client.

### Flow 2: CSV Dataset Ingestion
1. Client calls `POST /api/import`.
2. `DatasetImportController` invokes `DatasetImportService.importDataset()`.
3. `DatasetImportService` loads `classpath:dataset/creditcard.csv` via Spring `ResourceLoader`.
4. Streams lines row-by-row using `OpenCSV` `CSVReader`:
   - Parses row 0 as `datasetTime`, row 29 as `amount`, row 30 as `actualFraud`.
   - Calls `transactionRepository.save(transaction)`.
   - Parses columns 1 to 28 into `TransactionFeatures` and calls `transactionFeaturesRepository.save(features)`.
   - Logs progress to stdout every 10,000 records.

### Flow 3: Dynamic Specification Search
1. Client calls `GET /api/transactions/search?minAmount=50&maxAmount=500&fraud=true&page=0&size=10`.
2. `TransactionController.searchTransactions()` validates request params (`@PositiveOrZero`, `@Min`).
3. `TransactionService` builds `Specification<Transaction>` using `TransactionSpecification.hasAmountBetween()` and `hasFraudStatus()`.
4. `TransactionRepository.findAll(specification, pageable)` executes paginated SQL query.
5. Entity results are mapped to `TransactionResponseDto` and returned as `Page<TransactionResponseDto>`.

---

## 4. Database Schema Reference

```sql
-- 1. Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    dataset_time BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    actual_fraud BOOLEAN NOT NULL,
    processing_status VARCHAR(255) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);

-- 2. Transaction features table
CREATE TABLE transaction_features (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE REFERENCES transactions(id) ON DELETE CASCADE,
    v1 DOUBLE PRECISION NOT NULL,
    v2 DOUBLE PRECISION NOT NULL,
    -- ... v3 through v27
    v28 DOUBLE PRECISION NOT NULL
);

-- 3. Predictions table
CREATE TABLE predictions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE REFERENCES transactions(id) ON DELETE CASCADE,
    risk_score DOUBLE PRECISION NOT NULL,
    severity VARCHAR(255) NOT NULL, -- LOW, MEDIUM, HIGH
    is_anomaly BOOLEAN NOT NULL,
    model_version VARCHAR(255) NOT NULL, -- V1
    predicted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

---

## 5. API Endpoint Reference

| Method | Endpoint | Controller | Request / Params | Response |
|---|---|---|---|---|
| `GET` | `/api/transactions` | `TransactionController` | `page=0, size=10, sortBy=id, direction=asc, fraud=(opt)` | `Page<TransactionResponseDto>` |
| `GET` | `/api/transactions/{id}` | `TransactionController` | `id` (path variable) | `TransactionResponseDto` |
| `GET` | `/api/transactions/search` | `TransactionController` | `minAmount, maxAmount, fraud, page, size, sortBy, direction` | `Page<TransactionResponseDto>` |
| `GET` | `/api/dashboard/summary` | `DashboardController` | None | `DashboardSummaryDto` |
| `GET` | `/api/dashboard/amount-summary` | `DashboardController` | None | `DashboardAmountSummaryDto` |
| `GET` | `/api/dashboard/recent-frauds` | `DashboardController` | None | `List<TransactionResponseDto>` |
| `GET` | `/api/dashboard/status` | `DashboardController` | None | `TransactionStatusSummaryDto` |
| `POST` | `/api/fraud/predict` | `FraudDetectionController` | Body: `FraudPredictionRequest` | `FraudPredictionResponse` |
| `POST` | `/api/import` | `DatasetImportController` | None | `String` ("Dataset imported successfully") |

---

## 6. How to Run and Test

### 1. Start PostgreSQL
Ensure PostgreSQL is running on `localhost:5432` with a database named `transaction_monitoring`.

### 2. Start FastAPI ML Microservice
```bash
cd ml-service
# Activate venv:
.\venv\Scripts\activate   # Windows
source venv/bin/activate  # Linux/Mac
# Launch server:
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
Verify via `curl http://127.0.0.1:8000/`. Expected response:
`{"service":"transaction-fraud-detection","status":"running"}`

### 3. Start Spring Boot Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run  # Windows
./mvnw spring-boot:run      # Linux/Mac
```
Verify via `curl http://localhost:8080/api/dashboard/summary`.

### 4. Test ML Endpoint via Spring Boot
```bash
curl -X POST http://localhost:8080/api/fraud/predict \
  -H "Content-Type: application/json" \
  -d "{\"Time\":0.0,\"Amount\":149.62,\"V1\":-1.3598,\"V2\":-0.0728,\"V3\":2.5363,\"V4\":1.3782,\"V5\":-0.3383,\"V6\":0.4624,\"V7\":0.2396,\"V8\":0.0987,\"V9\":0.3638,\"V10\":0.0908,\"V11\":-0.5516,\"V12\":-0.6178,\"V13\":-0.9914,\"V14\":-0.3112,\"V15\":1.4682,\"V16\":-0.4704,\"V17\":0.2080,\"V18\":0.0258,\"V19\":0.4040,\"V20\":0.2514,\"V21\":-0.0183,\"V22\":0.2778,\"V23\":-0.1105,\"V24\":0.0669,\"V25\":0.1285,\"V26\":-0.1891,\"V27\":0.1336,\"V28\":-0.0211}"
```

---

## 7. Known Bugs and High-Priority Gotchas

1. **Unbatched CSV Import:** `DatasetImportService.java` calls `save()` sequentially in a loop for 284,807 rows without `@Transactional`. It produces 569k individual SQL transactions and can take hours. Needs chunked batching (`saveAll`) and `entityManager.clear()`.
2. **Orphaned Predictions:** Inferences requested via `/api/fraud/predict` are never persisted to the `predictions` table. `PredictionRepository` is unused in active service code.
3. **Static Processing Status:** Every transaction is initialized as `PENDING` via `@PrePersist` in `Transaction.java` and is never updated to `PROCESSING` or `COMPLETED`.
4. **Swallowed Import Exceptions:** `DatasetImportService.java` catches exceptions with `e.printStackTrace()` and still returns success to the controller even on fatal errors.
5. **No Authentication or Rate Limiting:** All endpoints (including `/api/import`) are publicly exposed without Spring Security.
6. **Feature Mismatch in `train_model.py`:** `train_model.py` trains `IsolationForest` on 29 raw features, whereas `predict.py` expects 31 features including `Amount_Log`, `time_sin`, and `time_cos` for `fraud_model.pkl`.

---

## 8. Exact File References for Common Modifications

- **To adjust the ML classification threshold:** Edit [predict.py:24](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/predict.py#L24) or re-save `threshold.pkl`.
- **To persist predictions:** Modify [FraudDetectionService.java:22-32](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/FraudDetectionService.java#L22-L32) and inject `PredictionRepository`.
- **To fix CSV import batching:** Modify [DatasetImportService.java:50-65](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java#L50-L65).
- **To add new search filter parameters:** Add predicates in [TransactionSpecification.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/specifications/TransactionSpecification.java) and expose in [TransactionController.java:84-122](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java#L84-L122).
- **To configure Docker Compose:** Populate [docker-compose.yml](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/docker-compose.yml) and add Dockerfiles.

---

## 9. Recommended Next Engineering Steps (In Priority Order)

1. **Implement Ingestion Batching:** Refactor `DatasetImportService` to use batch chunks (e.g. 2,000 entities) and Spring transaction management.
2. **Wire ML Predictions to DB Persistence:** Update `FraudDetectionService` or create a transaction evaluation pipeline that saves predictions to PostgreSQL and advances transaction status to `COMPLETED`.
3. **Build the Web Frontend Dashboard:** Create a responsive dashboard in `frontend/` consuming `/api/dashboard/*` and `/api/transactions`.
4. **Complete Dockerization:** Provide multi-stage Dockerfiles for backend and ML service and configure `docker-compose.yml`.
5. **Implement Integration & Unit Tests:** Add `@WebMvcTest` and `@DataJpaTest` suites in `backend/src/test/`.
