# INTERVIEW PREPARATION GUIDE

This guide provides technical explanations, architecture walkthroughs, design trade-offs, and anticipated interview questions grounded directly in the implementation of the Transaction Monitoring System.

---

## 1. 60-Second Project Pitch

> "I designed and implemented an end-to-end Transaction Monitoring System that detects financial fraud by pairing a Java 21 Spring Boot backend with a Python FastAPI machine learning microservice. 
> 
> The backend handles large-scale transaction persistence in PostgreSQL, provides RESTful APIs with dynamic JPA criteria filtering, and aggregates compliance metrics like fraud dollar exposure and processing status distributions. 
> 
> For fraud detection, I engineered periodic cyclical time representations and log-scaled monetary features, trained a class-weighted Logistic Regression model on over 284,000 credit card records, and tuned the decision threshold to 0.95 to achieve an 80% fraud recall with low false alarms. 
> 
> The services communicate over synchronous HTTP via Spring's modern RestClient, exposing structured insights for operational fraud monitoring."

---

## 2. 3-Minute Technical Deep Dive

> "The core challenge this project addresses is identifying extremely sparse fraudulent events—less than 0.2% of total transactions—without degrading real-time API performance or generating unacceptable alert fatigue for risk analysts.
> 
> **Architecture & Data Flow:**
> The system is architected as two decoupled microservices. The backend is built with Spring Boot 4 and Java 21, connected to PostgreSQL. The schema is normalized into three core tables: `transactions` for audit and lifecycle tracking, `transaction_features` holding 28 PCA-transformed numerical signals (`V1` to `V28`), and `predictions` for ML inferences. 
> 
> **Machine Learning Pipeline:**
> For the detection engine, I utilized the Kaggle European Cardholders dataset. I handled class imbalance using two strategies: first evaluating unsupervised Isolation Forests, and then training a supervised `LogisticRegression` with `class_weight='balanced'`. Because transaction timestamps were relative seconds over 48 hours, I transformed them into cyclical time-of-day features using sine and cosine trigonometry across a 24-hour cycle, alongside a `log1p` transformation on transaction amounts. On an untouched test set of over 56,000 transactions, the model caught 80% of all fraudulent transactions (76 out of 95) with a tuned threshold of 0.95, keeping false positives down to just 109.
> 
> **Backend Integration & Querying:**
> I deployed the inference pipeline as a FastAPI microservice. The Spring Boot backend integrates with FastAPI using the modern `RestClient`. On the query side, I implemented Spring Data JPA Criteria Specifications in `TransactionSpecification.java`, allowing risk analysts to dynamically query transactions across variable amount ranges and fraud flags with pagination and sort directions without writing redundant boilerplate queries.
> 
> **Key Engineering Lessons:**
> Building this highlighted the difference between batch and OLTP workloads. For instance, importing 284,000 records row-by-row causes massive JDBC transaction overhead, which showed me the necessity of chunked batch processing and database indexing on timestamp and amount columns for high-throughput production systems."

---

## 3. Key Design Decisions and Trade-offs

### 1. Dual-Microservice Architecture (Spring Boot + FastAPI) vs. Monolith
- **Decision:** Split the platform into a Java Spring Boot backend and a Python FastAPI ML microservice.
- **Rationale:** Java provides enterprise reliability, mature ORM capabilities, and strict type safety for data access; Python is the industry standard for machine learning libraries (NumPy, Scikit-learn, Pandas).
- **Trade-off:** Introduces network latency over HTTP, requires managing two separate runtimes, and necessitates error-handling strategies for inter-service communication.

### 2. Normalized PCA Features Table (`transaction_features`) vs. Single Table or JSONB
- **Decision:** Created a separate `transaction_features` table with 28 explicit `DOUBLE PRECISION` columns linked by a foreign key.
- **Rationale:** Keeps the core `transactions` table narrow and cache-efficient for rapid status updates and dashboard aggregations. Enforces strict schema validation rather than arbitrary JSON structures.
- **Trade-off:** Requires joining tables when querying full feature vectors and adds foreign key insert overhead during batch ingestion.

### 3. Logistic Regression with Threshold Tuning vs. Isolation Forest
- **Decision:** Selected `LogisticRegression` with balanced class weights and a high decision threshold (0.95) over unsupervised `IsolationForest`.
- **Rationale:** `IsolationForest` suffered from excessive false positives in exploration. Logistic Regression with balanced weighting directly optimized class separation, and post-training threshold tuning on validation data allowed explicit control over the precision-recall trade-off.
- **Trade-off:** Supervised models depend heavily on labeled training data and may fail to identify novel, zero-day fraud patterns that unsupervised anomaly detectors might flag.

### 4. Cyclical Trigonometric Feature Engineering for Time
- **Decision:** Transformed linear elapsed seconds into `sin(2π * hour / 24)` and `cos(2π * hour / 24)`.
- **Rationale:** Time within a day is periodic (23:59 is adjacent to 00:01). Linear representations treat midnight and 23:59 as distant extremes, distorting distance-based and linear models.
- **Trade-off:** Adds computation overhead during ingestion and inference preprocessing.

### 5. Spring Data JPA Specifications vs. Native SQL
- **Decision:** Used `JpaSpecificationExecutor` and Criteria API predicates in `TransactionSpecification.java`.
- **Rationale:** Allows client APIs to dynamically combine optional filters (e.g. only minimum amount, only fraud status, or all combined) in a single endpoint without building fragile dynamic SQL string concatenations.
- **Trade-off:** More verbose Java code; slightly higher memory footprint than raw native SQL queries.

---

## 4. Top 10 Technical Interview Questions & Model Answers

### Q1: How did you handle the extreme class imbalance in the credit card dataset?
**Answer:** "The dataset contained 284,807 transactions with only 492 frauds (~0.17%). If a model simply predicted genuine for every transaction, it would achieve 99.83% accuracy while missing 100% of frauds. I addressed this by:
1. Using stratified train/test splits (`stratify=y`) to maintain the exact minority distribution across splits.
2. In model training, applying `class_weight='balanced'`, which penalizes misclassifications on the minority class inversely proportional to their class frequency.
3. Shifting evaluation from accuracy to Precision, Recall, Confusion Matrix, and F1-score.
4. Tuning the probability classification threshold using validation data to 0.95, which yielded an 80% recall on the test set."

### Q2: Why did you use `BigDecimal` for transaction amounts instead of `double` or `float`?
**Answer:** "In Java, `float` and `double` are IEEE 754 binary floating-point types that cannot represent base-10 decimal fractions (like $0.10 or $0.01) precisely, leading to cumulative rounding errors. For financial applications, `BigDecimal` provides arbitrary precision and predictable rounding modes, ensuring that monetary calculations and aggregations in `DashboardService` match financial accounting standards."

### Q3: Explain how your cyclical time transformation works and why it is necessary.
**Answer:** "The dataset's `Time` feature recorded elapsed seconds from the first recorded transaction. In fraud analysis, time of day is predictive (e.g., fraudulent activity spiking in late-night hours). However, linear time features fail because hour 23 and hour 0 are mathematically far apart despite being adjacent. I computed `hour = (time % 86400) / 3600`, transformed this into an angle on a unit circle `θ = 2π * hour / 24`, and generated two features: `time_sin = sin(θ)` and `time_cos = cos(θ)`. This maps continuous time onto a smooth 24-hour cycle."

### Q4: What happens if the Python ML microservice crashes or experiences network latency?
**Answer:** "In the current implementation, Spring's `RestClient` performs a synchronous blocking HTTP call in `FraudDetectionService.java`. If the ML service is down or times out, the call will throw an `HttpServerErrorException` or `ResourceAccessException`, resulting in an HTTP 500 error for the client. In a production refactoring, I would implement Resilience4j circuit breakers with fallback logic (such as queuing the transaction for asynchronous processing) or decouple inference completely using an asynchronous event broker like Kafka."

### Q5: What is the N+1 query problem, and how does your entity mapping handle it?
**Answer:** "The N+1 problem occurs when an application fetches $N$ entities and subsequently executes $N$ additional database queries to load their lazily-fetched related entities. In `Transaction.java`, both `transactionFeatures` and `prediction` are mapped with `FetchType.LAZY`. In our primary paginated list endpoint (`/api/transactions`), we only project to `TransactionResponseDto`, which accesses fields on the root `Transaction` table. This avoids triggering unnecessary queries for feature vectors or predictions."

### Q6: How does Spring Data JPA's Specification pattern work in your search endpoint?
**Answer:** "The `TransactionSpecification` class defines static methods returning `Specification<Transaction>`, which encapsulate JPA Criteria API predicates. For example, `hasAmountBetween(min, max)` uses `criteriaBuilder.between(root.get('amount'), min, max)`. In `TransactionService`, we start with `criteriaBuilder.conjunction()` (a logical TRUE) and chain predicates dynamically using `.and()` based on non-null query parameters. This compiles into a single SQL query with optimal `WHERE` clauses."

### Q7: Why did you select OpenCSV over standard Java I/O for dataset import?
**Answer:** "While a naive `BufferedReader.readLine().split(',')` works on simple CSVs, financial datasets often contain escaped strings, embedded commas, or quoted values that break manual splitting. OpenCSV handles CSV standards compliance, delimiter escaping, and quotes out of the box. Furthermore, by streaming records row-by-row via `CSVReader.readNext()`, we avoid loading the entire 147MB file into heap memory all at once."

### Q8: What database indexes would you add to optimize the dashboard queries?
**Answer:** "Currently, only primary and unique foreign keys are indexed. To optimize frequent queries:
1. `findTop10ByActualFraudTrueOrderByDatasetTimeDesc()`: Needs a composite index on `(actual_fraud, dataset_time DESC)` to satisfy both the filter and sort without a full-table scan.
2. `searchTransactions()`: Benefits from an index on `amount`.
3. `countTransactionsByProcessingStatus()`: An index on `processing_status` allows index-only group-by aggregations."

### Q9: What is the purpose of `@PrePersist` in JPA entities?
**Answer:** "The `@PrePersist` annotation marks callback methods in JPA entity lifecycle management that execute before the `EntityManager.persist()` operation issues an `INSERT` statement. In `Transaction.java`, we use it to automatically assign `createdAt = LocalDateTime.now()` and default `processingStatus = ProcessingStatus.PENDING`, ensuring audit timestamps and status invariants are set without relying on caller code."

### Q10: How does your global exception handler improve API consumer experience?
**Answer:** "Without centralized exception handling, unhandled exceptions return raw Spring stack traces with HTTP 500 statuses, leaking internal class details. `GlobalExceptionHandler` uses `@ControllerAdvice` and `@ExceptionHandler` to intercept specific errors—such as `TransactionNotFoundException` or `ConstraintViolationException`—and return consistent, sanitized JSON payloads containing timestamps, HTTP status codes, error descriptions, and user-friendly messages."

---

## 5. Five Challenging Follow-up Questions

1. **"Your model achieved 80% recall and 41% precision. If an executive asks you to cut investigator alert fatigue in half, how would you adjust the system?"**
   - *Answer:* "Precision is 41%, meaning ~59% of generated alerts are false positives. Because we saved our model with predicted probabilities, we can adjust the classification threshold upward from 0.95 toward 0.98. By plotting the Precision-Recall curve on validation data, we can identify the threshold where precision reaches 70–80%, accepting a slight reduction in recall (e.g., from 80% down to 72%) to cut investigator false alarms in half."

2. **"How would you re-architect the CSV import to handle a 10GB transaction feed without running out of memory or locking the database?"**
   - *Answer:* "First, move ingestion to an asynchronous batch service (Spring Batch). Second, split the input stream into chunks (e.g., 5,000 records). Third, use JDBC batch inserts (`rewriteBatchedStatements=true` in PostgreSQL) rather than JPA `save()` to perform bulk multi-row inserts. Fourth, clear the Hibernate persistence context after each chunk. Finally, execute ingestion in a staging table before bulk-merging into production tables."

3. **"How would you handle concept drift if fraud patterns change over time?"**
   - *Answer:* "Financial fraud techniques constantly evolve. I would implement an automated monitoring pipeline that logs input feature distributions and prediction outputs to PostgreSQL. Using statistical tests (like Population Stability Index or Kolmogorov-Smirnov tests), the system would alert when incoming feature distributions diverge from training distributions. I would also establish scheduled retraining pipelines using recent labeled transaction windows."

4. **"If multiple instances of the backend service run behind a load balancer, how does that affect your current implementation?"**
   - *Answer:* "The backend services are mostly stateless, which supports horizontal scaling. However, two areas require attention: first, `POST /api/import` is stateful and long-running; concurrent calls from multiple instances would cause duplicate inserts or deadlocks. We should introduce distributed locking (e.g., Redis Redlock or database-level advisory locks). Second, the connection to FastAPI would benefit from a load-balanced internal URL or service discovery rather than a hardcoded loopback address."

5. **"What security vulnerabilities exist in the current codebase, and how would you fix them before deploying to production?"**
   - *Answer:* "The key vulnerabilities are:
     1. Unauthenticated endpoints: Anyone can access transaction data or trigger heavy imports. I would implement Spring Security with OAuth2/JWT tokens.
     2. Hardcoded credentials: `application.properties` contains plaintext database passwords. I would externalize these to environment variables or secret vaults.
     3. CORS: No CORS policy is currently defined; I would add restrictive CORS configurations allowing only authorized frontend origins."
