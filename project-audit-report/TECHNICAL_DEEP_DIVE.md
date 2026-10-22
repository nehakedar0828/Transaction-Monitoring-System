# TECHNICAL DEEP DIVE

## 1. Backend Architecture & Package Structure

The backend source code is located under `backend/src/main/java/com/nehakedar/backend/`. It adheres to standard Spring Boot layered architectural conventions:

```
com.nehakedar.backend
├── BackendApplication.java           # Application entry point (@SpringBootApplication)
├── config
│   └── RestClientConfig.java         # Spring 6.1+ RestClient bean definition
├── constant
│   ├── ModelVersion.java             # Enum: V1
│   ├── ProcessingStatus.java         # Enum: PENDING, PROCESSING, COMPLETED, FAILED
│   └── Severity.java                 # Enum: LOW, HIGH, MEDIUM
├── controller
│   ├── DashboardController.java       # Aggregations, summaries, recent frauds, status counts
│   ├── DatasetImportController.java   # Trigger CSV import endpoint
│   ├── FraudDetectionController.java  # Proxy endpoint to ML service
│   └── TransactionController.java     # Paginated queries, ID lookup, dynamic specifications
├── dto
│   ├── DashboardAmountSummaryDto.java # Total, fraud, and genuine currency sums
│   ├── DashboardSummaryDto.java       # Counts, fraud volume, fraud percentage
│   ├── FraudPredictionRequest.java    # Schema matching FastAPI input payload
│   ├── FraudPredictionResponse.java   # Schema matching FastAPI prediction output
│   ├── TransactionCreateRequest.java  # DTO for manual transaction creation
│   ├── TransactionResponseDto.java    # Clean client-facing transaction projection
│   └── TransactionStatusSummaryDto.java# Status distribution map
├── entity
│   ├── Prediction.java                # Risk scores, severity, anomaly flag, model version
│   ├── Transaction.java               # Core transaction record with audit timestamps
│   └── TransactionFeatures.java       # Normalized PCA features (V1 through V28)
├── exception
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice for custom HTTP error mapping
│   └── TransactionNotFoundException.java # RuntimeException for missing entities
├── repository
│   ├── PredictionRepository.java      # JpaRepository<Prediction, Long>
│   ├── TransactionFeaturesRepository.java # JpaRepository<TransactionFeatures, Long>
│   └── TransactionRepository.java     # JpaRepository & JpaSpecificationExecutor
└── specifications
    └── TransactionSpecification.java  # JPA Criteria API predicate factories
```

---

## 2. Controllers & REST API Inventory

### Detailed Endpoint Inventory

| HTTP Method | Route / Endpoint | Controller Method | Purpose | Request Parameters / Body | Response Payload / Status | Verified Status |
|---|---|---|---|---|---|---|
| `GET` | `/api/transactions` | [TransactionController.java:23](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java#L23) | Retrieve paginated transactions with sorting and optional fraud status filter | `page` (int, def: 0, min: 0), `size` (int, def: 10, min: 1), `sortBy` (str, def: "id"), `direction` (str, def: "asc"), `fraud` (bool, optional) | `Page<TransactionResponseDto>` (HTTP 200) | **Implemented & Verified** |
| `GET` | `/api/transactions/{id}` | [TransactionController.java:65](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java#L65) | Fetch single transaction by primary key | `id` (Long, path variable) | `TransactionResponseDto` (HTTP 200) or error (HTTP 404) | **Implemented & Verified** |
| `GET` | `/api/transactions/search` | [TransactionController.java:84](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/TransactionController.java#L84) | Search transactions using dynamic criteria (amount range, fraud flag) | `minAmount` (BigDecimal, @PositiveOrZero), `maxAmount` (BigDecimal, @PositiveOrZero), `fraud` (Boolean), `page`, `size`, `sortBy`, `direction` | `Page<TransactionResponseDto>` (HTTP 200) | **Implemented & Verified** |
| `GET` | `/api/dashboard/summary` | [DashboardController.java:24](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DashboardController.java#L24) | Retrieve aggregate count and fraud percentage | None | `DashboardSummaryDto` (HTTP 200) | **Implemented & Verified** |
| `GET` | `/api/dashboard/amount-summary` | [DashboardController.java:32](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DashboardController.java#L32) | Retrieve monetary sums across transaction types | None | `DashboardAmountSummaryDto` (HTTP 200) | **Implemented & Verified** |
| `GET` | `/api/dashboard/recent-frauds` | [DashboardController.java:40](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DashboardController.java#L40) | Get the top 10 most recent fraudulent transactions | None | `List<TransactionResponseDto>` (HTTP 200) | **Implemented & Verified** |
| `GET` | `/api/dashboard/status` | [DashboardController.java:48](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DashboardController.java#L48) | Get count of transactions grouped by processing status | None | `TransactionStatusSummaryDto` (HTTP 200) | **Implemented & Verified** |
| `POST` | `/api/fraud/predict` | [FraudDetectionController.java:16](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/FraudDetectionController.java#L16) | Forward transaction features to Python FastAPI ML microservice | JSON body: `FraudPredictionRequest` (Time, V1..V28, Amount) | `FraudPredictionResponse` (fraud, probability, threshold) (HTTP 200) | **Implemented & Verified** |
| `POST` | `/api/import` | [DatasetImportController.java:16](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/controller/DatasetImportController.java#L16) | Ingest local `creditcard.csv` into database | None | `String`: "Dataset imported successfully" (HTTP 200) | **Implemented & Verified** |

---

## 3. Database Schema & Persistence Layer

The database schema is managed via Hibernate DDL auto-generation (`spring.jpa.hibernate.ddl-auto=update` in `application.properties`). No Flyway or Liquibase scripts are present.

### Physical Table Design

#### 1. `transactions`
Represents core transactional data and system lifecycle status.

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    dataset_time BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    actual_fraud BOOLEAN NOT NULL,
    processing_status VARCHAR(255) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);
```

- **Relationships:**
  - `1-to-1` with `transaction_features` (mapped by `transaction`, `cascade = CascadeType.ALL`, `FetchType.LAZY`).
  - `1-to-1` with `predictions` (mapped by `transaction`, `cascade = CascadeType.ALL`, `FetchType.LAZY`).
- **Entity Lifecycle Callbacks:**
  - `@PrePersist` sets `createdAt = LocalDateTime.now()` and `processingStatus = ProcessingStatus.PENDING`.

#### 2. `transaction_features`
Stores the 28 principal component analysis (PCA) features.

```sql
CREATE TABLE transaction_features (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    v1 DOUBLE PRECISION NOT NULL,
    v2 DOUBLE PRECISION NOT NULL,
    ...
    v28 DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_transaction_features_transaction 
        FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);
```

#### 3. `predictions`
Stores model inferences, risk scores, and assessment severity.

```sql
CREATE TABLE predictions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    risk_score DOUBLE PRECISION NOT NULL,
    severity VARCHAR(255) NOT NULL, -- LOW, MEDIUM, HIGH
    is_anomaly BOOLEAN NOT NULL,
    model_version VARCHAR(255) NOT NULL, -- V1
    predicted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_predictions_transaction 
        FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);
```

- **Entity Lifecycle Callbacks:**
  - `@PrePersist` sets `predictedAt = LocalDateTime.now()`.

### Repository Layer Analysis

- **[TransactionRepository.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/repository/TransactionRepository.java):**
  - Inherits `JpaRepository<Transaction, Long>` and `JpaSpecificationExecutor<Transaction>`.
  - `findByActualFraud(Boolean actualFraud, Pageable pageable)`: Generates paginated queries with boolean filtering.
  - `findTotalAmount()`: `@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")`.
  - `findFraudAmount()`: `@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.actualFraud = true")`.
  - `findGenuineAmount()`: `@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.actualFraud = false")`.
  - `findTop10ByActualFraudTrueOrderByDatasetTimeDesc()`: Fetches the 10 most recent frauds.
  - `countTransactionsByProcessingStatus()`: Group-by aggregation query returning `List<Object[]>`.

---

## 4. Service-Layer Business Logic

### [TransactionService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/TransactionService.java)
- **`getTransactions(page, size, sortBy, direction, fraud)`:**
  Constructs a Spring `Pageable` instance based on requested sort fields. Conditionally executes `findAll(pageable)` when `fraud` is null, or `findByActualFraud(fraud, pageable)` when specified. Transforms entity pages using DTO projection `mapToDto`.
- **`getTransactionById(id)`:**
  Queries `findById(id)`. Throws `TransactionNotFoundException` if empty; otherwise returns `TransactionResponseDto`.
- **`searchTransactions(minAmount, maxAmount, fraud, page, size, sortBy, direction)`:**
  Uses JPA Criteria Specifications:
  ```java
  Specification<Transaction> specification = (root, query, cb) -> cb.conjunction();
  if (minAmount != null || maxAmount != null) {
      specification = specification.and(TransactionSpecification.hasAmountBetween(minAmount, maxAmount));
  }
  if (fraud != null) {
      specification = specification.and(TransactionSpecification.hasFraudStatus(fraud));
  }
  return transactionRepository.findAll(specification, pageable).map(this::mapToDto);
  ```

### [DashboardService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DashboardService.java)
- **`getSummary()`:**
  Executes `count()` and `countByActualFraud(true)`. Calculates `genuineTransactions = total - fraud` and `fraudPercentage = (fraud * 100.0) / total`. Guards against division by zero.
- **`getAmountSummary()`:**
  Executes database queries for total, fraud, and genuine currency volume.
- **`getStatusSummary()`:**
  Executes `countTransactionsByProcessingStatus()` and parses object arrays into a `Map<String, Long>`.

### [FraudDetectionService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/FraudDetectionService.java)
- **`predict(FraudPredictionRequest)`:**
  Invokes the external ML microservice at `${ml.service.url}/predict` via Spring `RestClient.post()` synchronously.
- **`predictTransaction(Transaction)`:**
  Extracts features from the entity, checks for nullity, constructs `FraudPredictionRequest`, and delegates to `predict()`.
  *(Note: This method is currently an orphaned helper and not invoked by any automatic processing flow).*

### [DatasetImportService.java](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/backend/src/main/java/com/nehakedar/backend/service/DatasetImportService.java)
- Reads `classpath:dataset/creditcard.csv` via Spring `ResourceLoader`.
- Uses `CSVReader.readNext()` to stream rows one by one.
- Saves `Transaction` and `TransactionFeatures` to their respective repositories.

---

## 5. Machine Learning Microservice Deep Dive

The ML microservice is written in Python and resides in `ml-service/`.

### Microservice API (`app/main.py`)
- **Framework:** FastAPI with Uvicorn.
- **Pydantic Model:** `TransactionRequest` validates 30 float fields (`Time`, `V1`..`V28`, `Amount`).
- **Endpoints:**
  - `GET /`: Returns `{"service": "transaction-fraud-detection", "status": "running"}`.
  - `POST /predict`: Calls `predict_transaction(transaction)` in `predict.py` and returns:
    ```json
    {
      "fraud": true,
      "probability": 0.9842,
      "threshold": 0.95
    }
    ```

### Feature Engineering & Inference Pipeline (`app/predict.py`)
The prediction module executes deterministic transformations matching the training notebook:
1. **Amount Transformation:**
   $$\text{Amount\_Log} = \ln(1 + \text{Amount})$$
2. **Cyclical Time Encoding:**
   Elapsed dataset seconds are mapped to a periodic 24-hour cycle:
   $$\text{time\_of\_day} = \text{Time} \pmod{86400}$$
   $$\text{hour} = \frac{\text{time\_of\_day}}{3600}$$
   $$\text{hour\_angle} = \frac{2\pi \cdot \text{hour}}{24}$$
   $$\text{time\_sin} = \sin(\text{hour\_angle}), \quad \text{time\_cos} = \cos(\text{hour\_angle})$$
3. **Feature Assembly:**
   Combines `[V1..V28, Amount_Log, time_sin, time_cos]` into a 31-dimensional feature vector.
4. **Standardization:**
   Passes the DataFrame into `scaler.transform()` using the saved `StandardScaler`.
5. **Probability Scoring & Classification:**
   Computes class probability $P(\text{fraud}) = \text{model.predict\_proba}(X)[0][1]$.
   Applies threshold rule:
   $$\text{is\_fraud} = P(\text{fraud}) \ge 0.95$$

### Model Exploration & Training Notebooks
- **Notebook:** [preprocessing.ipynb](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/notebooks/preprocessing.ipynb)
- **Exploration Script:** [eda.py](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/eda.py)
- **Standalone Anomaly Script:** [train_model.py](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/ml-service/app/train_model.py) evaluates `IsolationForest(n_estimators=200, contamination=fraud_ratio)` but this model was not selected for production inference.
- **Selected Model:** `LogisticRegression(class_weight="balanced", max_iter=1000, random_state=42)`.
- **Validation Metrics on Untouched Test Set (Cell 55 of notebook):**
  - Confusion Matrix:
    - True Negatives: **56,542**
    - False Positives: **109**
    - False Negatives: **19**
    - True Positives: **76**
  - **Recall:** **80.00%** (76 / 95 fraudulent transactions caught)
  - **Precision:** **41.08%** (76 / 185 alerts are true frauds)
  - **F1 Score:** **54.29%**
  - **Overall Accuracy:** **99.77%**

---

## 6. Dataset & CSV Import Implementation

### Dataset Specifications
- **File Location:** `backend/src/main/resources/dataset/creditcard.csv`
- **File Size:** ~147 MB (284,807 rows, 31 columns)
- **Columns:**
  - `Time` (Float): Seconds elapsed between each transaction and the first transaction in the dataset.
  - `V1` to `V28` (Float): Numerical attributes resulting from a Principal Component Analysis (PCA) transformation to protect user privacy.
  - `Amount` (Float): Transaction amount.
  - `Class` (Integer): Response variable (1 for fraudulent transactions, 0 for genuine transactions).
- **Target Distribution:**
  - Genuine (Class 0): **284,315** (99.827%)
  - Fraudulent (Class 1): **492** (0.173%)
  - Completely duplicated rows: **1,081** (removed during notebook training, but imported as-is by Java).

### Import Logic in `DatasetImportService.java`
```java
public void importDataset() {
    Resource resource = resourceLoader.getResource("classpath:dataset/creditcard.csv");
    BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
    CSVReader csvReader = new CSVReader(reader);
    csvReader.readNext(); // skip header
    String[] row;
    int count = 0;
    while ((row = csvReader.readNext()) != null) {
        Transaction transaction = createTransaction(row);
        transactionRepository.save(transaction);
        TransactionFeatures features = createTransactionFeatures(row, transaction);
        transactionFeaturesRepository.save(features);
        count++;
    }
}
```

---

## 7. Frontend Status

The repository contains an empty `frontend/` directory. No web application code (HTML, CSS, JavaScript, React, or Chart.js) has been committed to the repository. The dashboard functionality is currently exposed exclusively through the backend REST endpoints under `/api/dashboard/*`.

---

## 8. Infrastructure, Deployment & Configuration

### Current Files
- **[docker-compose.yml](file:///c:/Users/NEHA/OneDrive/Desktop/Study/transaction-monitoring-system/docker-compose.yml):** Present in root directory but has a length of **0 bytes**.
- **Dockerfiles:** Neither `backend/Dockerfile` nor `ml-service/Dockerfile` exists.

### Local Execution Strategy

#### ML Microservice
```bash
cd ml-service
python -m venv venv
# On Windows:
.\venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

#### Spring Boot Backend
1. Ensure PostgreSQL is active on `localhost:5432` with database `transaction_monitoring`.
2. Configure credentials in `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/transaction_monitoring
   spring.datasource.username=postgres
   spring.datasource.password=admin
   ml.service.url=http://127.0.0.1:8000
   ```
3. Run using the Maven wrapper:
   ```bash
   cd backend
   # On Windows:
   .\mvnw.cmd spring-boot:run
   # On Linux/macOS:
   ./mvnw spring-boot:run
   ```
4. Trigger dataset ingestion:
   ```bash
   curl -X POST http://localhost:8080/api/import
   ```
