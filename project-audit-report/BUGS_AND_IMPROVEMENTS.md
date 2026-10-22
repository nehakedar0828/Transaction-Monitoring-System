# BUGS, TECHNICAL DEBT, AND IMPROVEMENT PLAN

This document categorizes findings based on direct code inspection. Each issue includes file references, evidence, impact, and actionable recommendations.

---

## List A: Confirmed Bugs and Implementation Defects

### 1. Ingestion Performance & Lack of Transaction Boundaries
- **Severity:** **Critical**
- **File & Method:** [DatasetImportService.java:32-73](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java#L32-L73) (`importDataset`)
- **Evidence:**
  ```java
  while ((row = csvReader.readNext()) != null) {
      Transaction transaction = createTransaction(row);
      transactionRepository.save(transaction);

      TransactionFeatures features = createTransactionFeatures(row, transaction);
      transactionFeaturesRepository.save(features);
      count++;
  }
  ```
- **Impact:**
  The `creditcard.csv` dataset contains **284,807 rows**. The loop executes `transactionRepository.save()` and `transactionFeaturesRepository.save()` sequentially for each row. Without `@Transactional` and without batching (`saveAll()`), Hibernate creates and commits a separate database transaction for every single entity. That produces **569,614 round-trip network transactions** to PostgreSQL. The import operation takes hours, risks connection timeouts, exhausts database connection pools, and leaves the database in a partially imported state if an error occurs mid-stream.
- **Suggested Fix:**
  1. Add `@Transactional` to chunked batch methods or use Spring Batch.
  2. Collect entities into lists of size 1,000 to 5,000, call `saveAll()`, and invoke `entityManager.flush()` and `entityManager.clear()` to prevent out-of-memory errors on Hibernate's first-level cache.
  3. Configure JDBC batching in `application.properties`:
     ```properties
     spring.jpa.properties.hibernate.jdbc.batch_size=1000
     spring.jpa.properties.hibernate.order_inserts=true
     ```

---

### 2. Scientific Notation Vulnerability in Integer Dataset Parsing
- **Severity:** **High**
- **File & Method:** [DatasetImportService.java:79-81](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java#L79-L81) (`createTransaction`)
- **Evidence:**
  ```java
  transaction.setDatasetTime((long) Double.parseDouble(row[0]));
  transaction.setAmount(new BigDecimal(row[29]));
  transaction.setActualFraud(Integer.parseInt(row[30]) == 1);
  ```
- **Impact:**
  While `Double.parseDouble(row[0])` handles floating-point scientific notation for `Time`, `Integer.parseInt(row[30])` throws `java.lang.NumberFormatException` if `Class` is formatted as scientific notation (e.g. `"1.0"`, `"1e0"`, `"0.0"`), or if trailing whitespace is present. A single unhandled parse error aborts the entire import loop because the `try/catch` wraps the entire `while` block rather than isolating row-level failures.
- **Suggested Fix:**
  Trim strings and parse numerically using a safe converter:
  ```java
  boolean isFraud = Math.round(Double.parseDouble(row[30].trim())) == 1;
  transaction.setActualFraud(isFraud);
  ```
  Wrap row parsing in an inner try-catch to log malformed rows to an error queue rather than terminating the whole process.

---

### 3. Orphaned Predictions: ML Inferences Never Persisted
- **Severity:** **High**
- **File & Method:** [Prediction.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/entity/Prediction.java), [PredictionRepository.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/repository/PredictionRepository.java), [FraudDetectionService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/FraudDetectionService.java)
- **Evidence:**
  `PredictionRepository` is declared but never injected into any service. `FraudDetectionController.predict()` calls `fraudDetectionService.predict(request)` and returns `FraudPredictionResponse` directly to the HTTP caller without persisting the prediction, risk score, severity, or timestamp. The `predictions` table in PostgreSQL remains perpetually empty.
- **Impact:**
  Historical auditability is lost. Analysts cannot query past predictions or calculate drift metrics because the predictions are not saved.
- **Suggested Fix:**
  In `FraudDetectionService`, inject `PredictionRepository` and `TransactionRepository`. When predicting on an existing transaction, instantiate a `Prediction` entity, calculate `Severity` based on threshold bands (e.g., probability > 0.95 = `HIGH`, > 0.70 = `MEDIUM`, else `LOW`), set `isAnomaly = response.getFraud()`, and save to `predictionRepository`.

---

### 4. Processing Status Lifecycle Inconsistency
- **Severity:** **Medium**
- **File & Method:** [Transaction.java:46-50](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/entity/Transaction.java#L46-L50), [DashboardService.java:102-126](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DashboardService.java#L102-L126)
- **Evidence:**
  In `Transaction.java`:
  ```java
  @PrePersist
  public void onCreate() {
      this.createdAt = LocalDateTime.now();
      this.processingStatus = ProcessingStatus.PENDING;
  }
  ```
  Nowhere in the codebase is `processingStatus` ever updated to `PROCESSING`, `COMPLETED`, or `FAILED`.
- **Impact:**
  All transactions in the database remain in `PENDING` status indefinitely. When `DashboardController.getStatusSummary()` is called, the map contains only `{"PENDING": <total_count>}`. The `processedAt` timestamp column also remains `NULL` across all rows.
- **Suggested Fix:**
  Introduce an event-driven or worker-based status lifecycle. When a transaction is picked up for processing, set status to `PROCESSING`; once ML scoring completes and is persisted, update status to `COMPLETED` and record `processedAt = LocalDateTime.now()`.

---

### 5. Silent Ingestion Failure via Exception Printing
- **Severity:** **Medium**
- **File & Method:** [DatasetImportService.java:70-72](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java#L70-L72)
- **Evidence:**
  ```java
  } catch (Exception e) {
      e.printStackTrace();
  }
  ```
- **Impact:**
  If an exception occurs during import (e.g., file not found, PostgreSQL connection failure, constraint violation), the exception is swallowed by `e.printStackTrace()`. The HTTP caller still receives an HTTP 200 OK with `"Dataset imported successfully"` even if 0 rows were imported.
- **Suggested Fix:**
  Use an SLF4J logger (`log.error(...)`) and re-throw a custom `DatasetImportException` mapped to HTTP 500 in `GlobalExceptionHandler`.

---

## List B: Potential Bugs and Architectural Risks Requiring Verification

### 1. Inconsistent Model Training vs. Production Inference Features
- **Risk Level:** **High**
- **Files:** [train_model.py:29](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/train_model.py#L29), [predict.py:27-37](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/predict.py#L27-L37), [preprocessing.ipynb](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/notebooks/preprocessing.ipynb)
- **Details:**
  - `train_model.py` trains an `IsolationForest` on 29 features (`V1..V28` + raw `Amount`).
  - `predict.py` loads `fraud_model.pkl` (which is a `LogisticRegression` model trained in `preprocessing.ipynb`) and expects **31 features** (`V1..V28`, `Amount_Log`, `time_sin`, `time_cos`).
  - If a developer executes `train_model.py` expecting it to update `fraud_model.pkl`, it does not save anything and uses entirely different feature inputs.
- **Action Required:** Align `train_model.py` with the 31-feature pipeline implemented in `preprocessing.ipynb` and save both models under clear versioned names.

---

### 2. Open Endpoints with Zero Authentication & Authorization
- **Risk Level:** **High**
- **Files:** All Controllers in `backend/src/main/java/com/nehakedar/backend/controller/`
- **Details:**
  All endpoints (`/api/transactions/**`, `/api/dashboard/**`, `/api/fraud/**`, `/api/import`) are completely public without Spring Security, JWT, or API keys. Anyone on the network can trigger a heavy 284k-record CSV import (`POST /api/import`) repeatedly, causing Denial of Service (DoS).
- **Action Required:** Introduce Spring Security with JWT or API Key authentication; restrict `/api/import` to administrative roles.

---

### 3. Synchronous HTTP ML Inference Bottleneck
- **Risk Level:** **Medium**
- **Files:** [FraudDetectionService.java:22-32](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/FraudDetectionService.java#L22-L32)
- **Details:**
  Spring's `RestClient` performs a blocking synchronous HTTP call to `http://127.0.0.1:8000/predict`. Under high transaction concurrency, threads will block waiting on Python process I/O. If the ML service is down or slow, transactions will hang or fail without fallback or circuit breaking.
- **Action Required:** Integrate Resilience4j for circuit breaking and retries, or transition to asynchronous message queues (RabbitMQ/Kafka).

---

### 4. Database Indexing Deficiencies on High-Cardinality Queries
- **Risk Level:** **Medium**
- **Files:** [Transaction.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/entity/Transaction.java)
- **Details:**
  Queries filter and sort on `amount`, `actual_fraud`, `processing_status`, and `dataset_time`. Currently, only primary keys and the foreign keys have unique indexes. Table scans on 285k+ rows for `/api/transactions/search` and `findTop10ByActualFraudTrueOrderByDatasetTimeDesc()` will degrade query latency.
- **Action Required:** Add composite and partial indexes in PostgreSQL:
  ```sql
  CREATE INDEX idx_transactions_actual_fraud_time ON transactions(actual_fraud, dataset_time DESC);
  CREATE INDEX idx_transactions_amount ON transactions(amount);
  CREATE INDEX idx_transactions_status ON transactions(processing_status);
  ```

---

## List C: Recommended Improvements and Roadmap

### 1. Complete Docker & Container Orchestration
- **Priority:** **High (Production Readiness)**
- **Target:** Populate [docker-compose.yml](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/docker-compose.yml) and add multi-stage `Dockerfile` for `backend` and `ml-service`.
- **Target Topology:**
  - `postgres` (PostgreSQL 16-alpine with healthcheck)
  - `backend` (Eclipse Temurin 21 JRE, port 8080)
  - `ml-service` (Python 3.11/3.13-slim, port 8000)

---

### 2. Build the Real-Time Monitoring Dashboard
- **Priority:** **High (Portfolio Presentation)**
- **Target:** Build a modern, responsive web dashboard in `frontend/` using Vanilla CSS / Tailwind and modern JavaScript / React with Chart.js.
- **Required Views:**
  - Metric cards: Total Transactions, Total Fraud Detected, Fraud Percentage, Total Dollars at Risk.
  - Interactive charts: Hourly fraud distribution, amount histograms.
  - Live transactions table with server-side pagination, status badges, and search filters.
  - "Run Fraud Inspection" modal to test arbitrary feature vectors against `/api/fraud/predict`.

---

### 3. Implement the Transaction Replay Engine
- **Priority:** **Medium**
- **Target:** Implement a scheduled Spring task or Kafka producer that replays records from `creditcard.csv` with a configurable delay (e.g., 50ms) to simulate a real-time transaction firehose, triggering automated ML evaluation and live WebSocket updates.

---

### 4. Automated Testing & Code Quality
- **Priority:** **Medium**
- **Target:** Expand [BackendApplicationTests.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/test/java/com/nehakedar/backend/BackendApplicationTests.java) with:
  - `TransactionControllerTest`: `@WebMvcTest` verifying pagination and 404 responses.
  - `TransactionServiceTest`: `@ExtendWith(MockitoExtension.class)` testing business logic and specifications.
  - `FraudDetectionServiceTest`: `MockRestServiceServer` testing client timeout and error resilience.
  - Python tests (`pytest` in `ml-service/tests/`) verifying model output formats and transformation accuracy.
